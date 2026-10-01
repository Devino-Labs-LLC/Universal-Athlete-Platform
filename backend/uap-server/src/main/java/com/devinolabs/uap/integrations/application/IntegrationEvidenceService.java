package com.devinolabs.uap.integrations.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.integrations.api.connections.EvidenceBatchResultView;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidencePort;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidenceView;
import com.devinolabs.uap.integrations.domain.ActiveConnectionPolicy;
import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.EvidenceId;
import com.devinolabs.uap.integrations.domain.IngestEvent;
import com.devinolabs.uap.integrations.domain.IngestEventId;
import com.devinolabs.uap.integrations.domain.IngestProcessingStatus;
import com.devinolabs.uap.integrations.domain.ProvenanceClass;
import com.devinolabs.uap.integrations.domain.SignalFamily;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

/**
 * V5A evidence upsert + inbox (ADR-047 / ADR-051). Store-only — no readiness / State Engine calls.
 *
 * <p>Initial ingest rejects {@code observedAt} older than {@code uap.integrations.backfill-days}.
 */
@Service
public class IntegrationEvidenceService implements ConnectedEvidencePort {

	private final ConnectionRepository connectionRepository;
	private final EvidenceRepository evidenceRepository;
	private final IngestEventRepository ingestEventRepository;
	private final SyncRunRepository syncRunRepository;
	private final IntegrationEvidencePersistenceGateway persistenceGateway;
	private final AthleteContextPort athleteContextPort;
	private final IntegrationsProperties properties;
	private final SecurityAuditWriter auditWriter;
	private final Clock clock;

	public IntegrationEvidenceService(
			ConnectionRepository connectionRepository,
			EvidenceRepository evidenceRepository,
			IngestEventRepository ingestEventRepository,
			SyncRunRepository syncRunRepository,
			IntegrationEvidencePersistenceGateway persistenceGateway,
			AthleteContextPort athleteContextPort,
			IntegrationsProperties properties,
			SecurityAuditWriter auditWriter,
			Clock clock) {
		this.connectionRepository = Objects.requireNonNull(connectionRepository);
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.ingestEventRepository = Objects.requireNonNull(ingestEventRepository);
		this.syncRunRepository = Objects.requireNonNull(syncRunRepository);
		this.persistenceGateway = Objects.requireNonNull(persistenceGateway);
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.properties = Objects.requireNonNull(properties);
		this.auditWriter = Objects.requireNonNull(auditWriter);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ConnectedEvidenceView> listEvidence(
			UUID athleteId,
			Instant fromInclusive,
			Instant toInclusive,
			String signalFamily) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(fromInclusive, "fromInclusive must not be null");
		Objects.requireNonNull(toInclusive, "toInclusive must not be null");
		if (toInclusive.isBefore(fromInclusive)) {
			throw new IntegrationValidationException("VALIDATION_ERROR", "to must not be before from");
		}
		SignalFamily family = parseOptionalFamily(signalFamily);
		return evidenceRepository
				.findByAthleteIdAndObservedAtBetween(athleteId, fromInclusive, toInclusive, family)
				.stream()
				.map(this::toView)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ConnectedEvidenceView> listEvidenceForConnection(
			UUID accountId,
			UUID connectionId,
			Instant fromInclusive,
			Instant toInclusive,
			String signalFamily) {
		Objects.requireNonNull(fromInclusive, "from must not be null");
		Objects.requireNonNull(toInclusive, "to must not be null");
		if (toInclusive.isBefore(fromInclusive)) {
			throw new IntegrationValidationException("VALIDATION_ERROR", "to must not be before from");
		}
		Connection connection = requireOwnedConnection(accountId, connectionId, false);
		SignalFamily family = parseOptionalFamily(signalFamily);
		return evidenceRepository
				.findByConnectionIdAndObservedAtBetween(connection.id(), fromInclusive, toInclusive, family)
				.stream()
				.map(this::toView)
				.toList();
	}

	@Transactional
	public EvidenceBatchResultView uploadEvidenceBatch(
			UUID accountId,
			UUID connectionId,
			UUID requestId,
			List<EvidenceBatchItemCommand> items) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Objects.requireNonNull(items, "items must not be null");
		assertModuleEnabled();

		Connection connection = requireOwnedConnection(accountId, connectionId, true);
		if (!connection.acceptsIngest()) {
			throw new IntegrationConflictException(
					"INTEGRATION_INGEST_NOT_ACCEPTED",
					"Evidence ingest is only accepted while the connection is CONNECTED");
		}

		String providerEventId = requestId.toString();
		Optional<IngestEvent> existingEvent = ingestEventRepository.findByProviderAndProviderEventId(
				connection.provider(),
				providerEventId);
		if (existingEvent.isPresent()) {
			return replayResult(connection, existingEvent.get(), requestId);
		}

		IngestEvent ingestEvent = IngestEvent.beginReceived(
				IngestEventId.generate(),
				connection.provider(),
				providerEventId,
				IngestEvent.EVENT_TYPE_EVIDENCE_BATCH,
				connection.id(),
				connection.athleteId(),
				clock);
		try {
			persistenceGateway.insertIngestEvent(ingestEvent);
		}
		catch (DataIntegrityViolationException race) {
			IngestEvent raced = ingestEventRepository
					.findByProviderAndProviderEventId(connection.provider(), providerEventId)
					.orElseThrow(() -> race);
			return replayResult(connection, raced, requestId);
		}

		SyncRunId syncRunId = SyncRunId.of(requestId);
		Optional<SyncRun> existingRun = syncRunRepository.findById(syncRunId);
		if (existingRun.isPresent()) {
			SyncRun prior = existingRun.get();
			if (!prior.connectionId().equals(connection.id())
					|| !prior.athleteId().equals(connection.athleteId())) {
				throw new IntegrationConnectionNotFoundException();
			}
			return toBatchResult(requestId, prior, true);
		}

		SyncRun syncRun = SyncRun.request(syncRunId, connection.id(), connection.athleteId(), clock);
		int accepted = 0;
		int rejected = 0;

		for (EvidenceBatchItemCommand item : items) {
			try {
				upsertItem(connection, syncRunId, item);
				accepted++;
			}
			catch (IllegalArgumentException | IntegrationValidationException ex) {
				rejected++;
			}
		}

		syncRun.completeUploadBatch(accepted, rejected, clock);
		if (accepted > 0) {
			connection.recordSuccessfulSync(clock);
		}
		else {
			connection.recordSyncAttempt(clock);
		}

		SyncRun savedRun = syncRunRepository.save(syncRun);
		connectionRepository.save(connection);
		ingestEvent.markProcessed(savedRun.id(), clock);
		persistenceGateway.saveIngestEvent(ingestEvent);

		audit(accountId, connection.athleteId(), connection.id().value(), "INTEGRATION_EVIDENCE_BATCH",
				"{\"syncRunId\":\"" + savedRun.id().value()
						+ "\",\"accepted\":" + accepted
						+ ",\"rejected\":" + rejected
						+ ",\"replayed\":false}");

		return toBatchResult(requestId, savedRun, false);
	}

	private EvidenceBatchResultView replayResult(Connection connection, IngestEvent event, UUID requestId) {
		if (event.connectionId() != null && !event.connectionId().equals(connection.id())) {
			throw new IntegrationConnectionNotFoundException();
		}
		if (!event.athleteId().equals(connection.athleteId())) {
			throw new IntegrationConnectionNotFoundException();
		}
		if (event.processingStatus() == IngestProcessingStatus.RECEIVED
				|| event.processingStatus() == IngestProcessingStatus.FAILED) {
			throw new IntegrationConflictException(
					"INTEGRATION_INGEST_IN_PROGRESS",
					"Evidence batch with this requestId is not yet complete; retry shortly");
		}
		SyncRunId syncRunId = event.syncRunId() != null ? event.syncRunId() : SyncRunId.of(requestId);
		SyncRun syncRun = syncRunRepository.findById(syncRunId)
				.orElseThrow(IntegrationConnectionNotFoundException::new);
		return toBatchResult(requestId, syncRun, true);
	}

	private void upsertItem(Connection connection, SyncRunId syncRunId, EvidenceBatchItemCommand item) {
		Objects.requireNonNull(item, "item must not be null");
		SignalFamily family = parseRequiredFamily(item.signalFamily());
		ProvenanceClass provenance = item.provenanceClass() == null || item.provenanceClass().isBlank()
				? ProvenanceClass.CLIENT_DEVICE
				: parseProvenance(item.provenanceClass());

		ConnectedEvidence.assertAcceptableObservationTime(
				item.observedAt(),
				item.providerUpdatedAt(),
				clock,
				properties.getBackfillDays());

		ConnectedEvidence candidate = ConnectedEvidence.create(
				EvidenceId.generate(),
				connection.athleteId(),
				connection.id(),
				connection.provider(),
				family,
				item.signalType(),
				item.externalRecordId(),
				item.valueNumeric(),
				item.valueText(),
				item.unitCode(),
				item.periodStart(),
				item.periodEnd(),
				item.observedAt(),
				item.providerUpdatedAt(),
				syncRunId,
				provenance,
				item.sourceDeviceOrApp(),
				null,
				clock);

		persistenceGateway.upsertEvidence(connection, candidate);
	}

	private Connection requireOwnedConnection(UUID accountId, UUID connectionId, boolean forUpdate) {
		AthleteRef athlete = forUpdate
				? athleteContextPort.requireMutableAthleteForUpdate(accountId)
				: athleteContextPort.requireAthlete(accountId);
		Connection connection = connectionRepository.findById(ConnectionId.of(connectionId))
				.orElseThrow(IntegrationConnectionNotFoundException::new);
		if (!ActiveConnectionPolicy.owns(connection, accountId, athlete.athleteId())) {
			throw new IntegrationConnectionNotFoundException();
		}
		return connection;
	}

	private void assertModuleEnabled() {
		if (!properties.isEnabled()) {
			throw new IntegrationProviderDisabledException(
					"INTEGRATIONS_DISABLED",
					"Integrations module is disabled");
		}
	}

	private void audit(UUID accountId, UUID athleteId, UUID resourceId, String eventType, String metadataJson) {
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				accountId,
				accountId,
				athleteId,
				null,
				null,
				"INTEGRATION_CONNECTION",
				resourceId,
				metadataJson));
	}

	private ConnectedEvidenceView toView(ConnectedEvidence evidence) {
		return new ConnectedEvidenceView(
				evidence.id().value(),
				evidence.connectionId().value(),
				evidence.provider().name(),
				evidence.signalFamily().name(),
				evidence.signalType(),
				evidence.externalRecordId(),
				evidence.valueNumeric(),
				evidence.valueText(),
				evidence.unitCode(),
				evidence.periodStart(),
				evidence.periodEnd(),
				evidence.observedAt(),
				evidence.providerUpdatedAt(),
				evidence.ingestedAt(),
				evidence.syncRunId() == null ? null : evidence.syncRunId().value(),
				evidence.provenanceClass().name(),
				evidence.sourceDeviceOrApp(),
				evidence.qualityCode(),
				evidence.status().name());
	}

	private static EvidenceBatchResultView toBatchResult(UUID requestId, SyncRun syncRun, boolean replayed) {
		return new EvidenceBatchResultView(
				requestId,
				syncRun.id().value(),
				syncRun.recordsAccepted(),
				syncRun.recordsRejected(),
				replayed);
	}

	private static SignalFamily parseRequiredFamily(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("signalFamily is required");
		}
		try {
			return SignalFamily.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown signalFamily: " + raw);
		}
	}

	private static SignalFamily parseOptionalFamily(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return SignalFamily.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IntegrationValidationException("VALIDATION_ERROR", "Unknown signalFamily: " + raw);
		}
	}

	private static ProvenanceClass parseProvenance(String raw) {
		try {
			return ProvenanceClass.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown provenanceClass: " + raw);
		}
	}

	/**
	 * Validated batch line item (application command — not an HTTP DTO).
	 */
	public record EvidenceBatchItemCommand(
			String externalRecordId,
			String signalFamily,
			String signalType,
			BigDecimal valueNumeric,
			String valueText,
			String unitCode,
			Instant periodStart,
			Instant periodEnd,
			Instant observedAt,
			Instant providerUpdatedAt,
			String sourceDeviceOrApp,
			String provenanceClass) {
	}
}
