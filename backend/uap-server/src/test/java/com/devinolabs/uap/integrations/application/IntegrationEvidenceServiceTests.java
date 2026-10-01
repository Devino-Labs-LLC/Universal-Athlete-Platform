package com.devinolabs.uap.integrations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.integrations.api.connections.EvidenceBatchResultView;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.IngestEvent;
import com.devinolabs.uap.integrations.domain.IngestEventId;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

class IntegrationEvidenceServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final UUID ACCOUNT = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
	private static final UUID ATHLETE = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
	private static final UUID CONNECTION = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

	private ConnectionRepository connectionRepository;
	private EvidenceRepository evidenceRepository;
	private IngestEventRepository ingestEventRepository;
	private SyncRunRepository syncRunRepository;
	private IntegrationEvidencePersistenceGateway persistenceGateway;
	private AthleteContextPort athleteContextPort;
	private IntegrationsProperties properties;
	private SecurityAuditWriter auditWriter;
	private IntegrationEvidenceService service;

	@BeforeEach
	void setUp() {
		connectionRepository = mock(ConnectionRepository.class);
		evidenceRepository = mock(EvidenceRepository.class);
		ingestEventRepository = mock(IngestEventRepository.class);
		syncRunRepository = mock(SyncRunRepository.class);
		persistenceGateway = mock(IntegrationEvidencePersistenceGateway.class);
		athleteContextPort = mock(AthleteContextPort.class);
		properties = new IntegrationsProperties();
		properties.setEnabled(true);
		properties.setBackfillDays(30);
		auditWriter = mock(SecurityAuditWriter.class);
		service = new IntegrationEvidenceService(
				connectionRepository,
				evidenceRepository,
				ingestEventRepository,
				syncRunRepository,
				persistenceGateway,
				athleteContextPort,
				properties,
				auditWriter,
				CLOCK);
	}

	@Test
	void uploadBatchAcceptsValidItemAndAudits() {
		Connection connected = connectedOsHub();
		UUID requestId = UUID.randomUUID();
		stubOwnedMutable(connected);
		when(ingestEventRepository.findByProviderAndProviderEventId(any(), any())).thenReturn(Optional.empty());
		when(syncRunRepository.findById(any())).thenReturn(Optional.empty());
		when(syncRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.insertIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.saveIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));

		EvidenceBatchResultView result = service.uploadEvidenceBatch(
				ACCOUNT,
				CONNECTION,
				requestId,
				List.of(validItem("ext-1", "420", "MINUTE", Instant.parse("2026-09-29T06:00:00Z"))));

		assertThat(result.acceptedCount()).isEqualTo(1);
		assertThat(result.rejectedCount()).isEqualTo(0);
		assertThat(result.replayed()).isFalse();
		verify(persistenceGateway).upsertEvidence(eq(connected), any());
		verify(auditWriter).append(any());
	}

	@Test
	void uploadBatchCountsValidationRejectsWithoutFailingBatch() {
		Connection connected = connectedOsHub();
		UUID requestId = UUID.randomUUID();
		stubOwnedMutable(connected);
		when(ingestEventRepository.findByProviderAndProviderEventId(any(), any())).thenReturn(Optional.empty());
		when(syncRunRepository.findById(any())).thenReturn(Optional.empty());
		when(syncRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.insertIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.saveIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));
		doThrow(new IntegrationValidationException("VALIDATION_ERROR", "bad"))
				.when(persistenceGateway)
				.upsertEvidence(any(), any());

		EvidenceBatchResultView result = service.uploadEvidenceBatch(
				ACCOUNT,
				CONNECTION,
				requestId,
				List.of(validItem("ext-bad", "1", "BPM", Instant.parse("2026-09-29T06:00:00Z"))));

		assertThat(result.acceptedCount()).isEqualTo(0);
		assertThat(result.rejectedCount()).isEqualTo(1);
	}

	@Test
	void uploadBatchRejectsStaleObservedAtAsItemReject() {
		Connection connected = connectedOsHub();
		UUID requestId = UUID.randomUUID();
		stubOwnedMutable(connected);
		when(ingestEventRepository.findByProviderAndProviderEventId(any(), any())).thenReturn(Optional.empty());
		when(syncRunRepository.findById(any())).thenReturn(Optional.empty());
		when(syncRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.insertIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));
		when(persistenceGateway.saveIngestEvent(any())).thenAnswer(inv -> inv.getArgument(0));

		Instant tooOld = NOW.minusSeconds(40L * 24 * 3600);
		EvidenceBatchResultView result = service.uploadEvidenceBatch(
				ACCOUNT,
				CONNECTION,
				requestId,
				List.of(validItem("ext-stale", "50", "BPM", tooOld)));

		assertThat(result.acceptedCount()).isEqualTo(0);
		assertThat(result.rejectedCount()).isEqualTo(1);
		verify(persistenceGateway, never()).upsertEvidence(any(), any());
	}

	@Test
	void uploadBatchRejectsWhenConnectionDisconnected() {
		Connection disconnected = connectedOsHub();
		disconnected.disconnect(CLOCK);
		stubOwnedMutable(disconnected);

		assertThatThrownBy(() -> service.uploadEvidenceBatch(
						ACCOUNT,
						CONNECTION,
						UUID.randomUUID(),
						List.of(validItem("ext-1", "1", "BPM", Instant.parse("2026-09-29T06:00:00Z")))))
				.isInstanceOf(IntegrationConflictException.class)
				.hasMessageContaining("CONNECTED");
	}

	@Test
	void uploadBatchReplaysCompletedIngestEvent() {
		Connection connected = connectedOsHub();
		UUID requestId = UUID.randomUUID();
		stubOwnedMutable(connected);

		IngestEvent prior = IngestEvent.beginReceived(
				IngestEventId.generate(),
				HealthProviderKey.APPLE_HEALTHKIT,
				requestId.toString(),
				IngestEvent.EVENT_TYPE_EVIDENCE_BATCH,
				connected.id(),
				ATHLETE,
				CLOCK);
		prior.markProcessed(SyncRunId.of(requestId), CLOCK);

		SyncRun priorRun = SyncRun.request(SyncRunId.of(requestId), connected.id(), ATHLETE, CLOCK);
		priorRun.completeUploadBatch(1, 0, CLOCK);

		when(ingestEventRepository.findByProviderAndProviderEventId(
						HealthProviderKey.APPLE_HEALTHKIT, requestId.toString()))
				.thenReturn(Optional.of(prior));
		when(syncRunRepository.findById(SyncRunId.of(requestId))).thenReturn(Optional.of(priorRun));

		EvidenceBatchResultView result = service.uploadEvidenceBatch(
				ACCOUNT,
				CONNECTION,
				requestId,
				List.of(validItem("ext-1", "420", "MINUTE", Instant.parse("2026-09-29T06:00:00Z"))));

		assertThat(result.replayed()).isTrue();
		assertThat(result.acceptedCount()).isEqualTo(1);
		verify(persistenceGateway, never()).insertIngestEvent(any());
		verify(persistenceGateway, never()).upsertEvidence(any(), any());
	}

	@Test
	void uploadBatchHandlesInsertRaceByReplaying() {
		Connection connected = connectedOsHub();
		UUID requestId = UUID.randomUUID();
		stubOwnedMutable(connected);

		IngestEvent raced = IngestEvent.beginReceived(
				IngestEventId.generate(),
				HealthProviderKey.APPLE_HEALTHKIT,
				requestId.toString(),
				IngestEvent.EVENT_TYPE_EVIDENCE_BATCH,
				connected.id(),
				ATHLETE,
				CLOCK);
		raced.markProcessed(SyncRunId.of(requestId), CLOCK);
		SyncRun priorRun = SyncRun.request(SyncRunId.of(requestId), connected.id(), ATHLETE, CLOCK);
		priorRun.completeUploadBatch(2, 0, CLOCK);

		when(ingestEventRepository.findByProviderAndProviderEventId(any(), any()))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(raced));
		doThrow(new DataIntegrityViolationException("dup"))
				.when(persistenceGateway)
				.insertIngestEvent(any());
		when(syncRunRepository.findById(SyncRunId.of(requestId))).thenReturn(Optional.of(priorRun));

		EvidenceBatchResultView result = service.uploadEvidenceBatch(
				ACCOUNT,
				CONNECTION,
				requestId,
				List.of());

		assertThat(result.replayed()).isTrue();
		assertThat(result.acceptedCount()).isEqualTo(2);
	}

	@Test
	void listEvidenceRejectsInvertedWindow() {
		assertThatThrownBy(() -> service.listEvidence(
						ATHLETE,
						Instant.parse("2026-09-30T00:00:00Z"),
						Instant.parse("2026-09-01T00:00:00Z"),
						null))
				.isInstanceOf(IntegrationValidationException.class);
	}

	@Test
	void foreignConnectionIsNotFound() {
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(CONNECTION))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.uploadEvidenceBatch(ACCOUNT, CONNECTION, UUID.randomUUID(), List.of()))
				.isInstanceOf(IntegrationConnectionNotFoundException.class);
	}

	private void stubOwnedMutable(Connection connection) {
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(athleteContextPort.requireAthlete(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(CONNECTION))).thenReturn(Optional.of(connection));
	}

	private Connection connectedOsHub() {
		Connection connection = Connection.beginPending(
				ConnectionId.of(CONNECTION),
				ATHLETE,
				ACCOUNT,
				HealthProviderKey.APPLE_HEALTHKIT,
				CLOCK);
		connection.confirmConnected(CLOCK);
		return connection;
	}

	private static IntegrationEvidenceService.EvidenceBatchItemCommand validItem(
			String externalId,
			String numeric,
			String unit,
			Instant observedAt) {
		return new IntegrationEvidenceService.EvidenceBatchItemCommand(
				externalId,
				"SLEEP",
				"DURATION",
				new BigDecimal(numeric),
				null,
				unit,
				null,
				null,
				observedAt,
				observedAt,
				"Apple Health",
				"CLIENT_DEVICE");
	}
}
