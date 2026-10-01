package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.EvidenceUpsertOutcome;
import com.devinolabs.uap.integrations.domain.IngestEvent;

/**
 * Isolated writes so unique-constraint races do not poison the outer batch transaction.
 */
@Component
class IntegrationEvidencePersistenceGateway {

	private final EvidenceRepository evidenceRepository;
	private final IngestEventRepository ingestEventRepository;
	private final Clock clock;

	IntegrationEvidencePersistenceGateway(
			EvidenceRepository evidenceRepository,
			IngestEventRepository ingestEventRepository,
			Clock clock) {
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.ingestEventRepository = Objects.requireNonNull(ingestEventRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IngestEvent insertIngestEvent(IngestEvent event) {
		return ingestEventRepository.save(event);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IngestEvent saveIngestEvent(IngestEvent event) {
		return ingestEventRepository.save(event);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void upsertEvidence(Connection connection, ConnectedEvidence candidate) {
		Optional<ConnectedEvidence> existing = evidenceRepository.findByProviderAndAthleteIdAndExternalRecordId(
				connection.provider(),
				connection.athleteId(),
				candidate.externalRecordId());

		if (existing.isEmpty()) {
			try {
				evidenceRepository.save(candidate);
				return;
			}
			catch (DataIntegrityViolationException race) {
				ConnectedEvidence raced = evidenceRepository
						.findByProviderAndAthleteIdAndExternalRecordId(
								connection.provider(),
								connection.athleteId(),
								candidate.externalRecordId())
						.orElseThrow(() -> race);
				merge(connection, raced, candidate);
				return;
			}
		}

		merge(connection, existing.get(), candidate);
	}

	private void merge(Connection connection, ConnectedEvidence current, ConnectedEvidence candidate) {
		if (!current.connectionId().equals(connection.id())) {
			throw new IntegrationValidationException(
					"INTEGRATION_EVIDENCE_FOREIGN_CONNECTION",
					"Evidence externalRecordId is bound to a different connection");
		}
		EvidenceUpsertOutcome outcome = current.applyIncoming(candidate, clock);
		if (outcome != EvidenceUpsertOutcome.IGNORED_STALE) {
			evidenceRepository.save(current);
		}
	}
}
