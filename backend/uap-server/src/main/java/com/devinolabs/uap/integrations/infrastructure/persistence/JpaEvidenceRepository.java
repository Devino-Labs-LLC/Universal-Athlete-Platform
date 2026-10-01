package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.EvidenceRepository;
import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SignalFamily;

@Repository
class JpaEvidenceRepository implements EvidenceRepository {

	private final IntegrationEvidenceJpaRepository jpaRepository;

	JpaEvidenceRepository(IntegrationEvidenceJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ConnectedEvidence save(ConnectedEvidence evidence) {
		Optional<IntegrationEvidenceJpaEntity> existing = jpaRepository.findById(evidence.id().value());
		IntegrationEvidenceJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(IntegrationEvidencePersistenceMapper.toEntity(evidence, true));
		}
		else {
			IntegrationEvidenceJpaEntity entity = existing.get();
			entity.applyDomainState(
					evidence.signalFamily(),
					evidence.signalType(),
					evidence.valueNumeric(),
					evidence.valueText(),
					evidence.unitCode(),
					evidence.periodStart(),
					evidence.periodEnd(),
					evidence.observedAt(),
					evidence.providerUpdatedAt(),
					evidence.ingestedAt(),
					evidence.syncRunId() == null ? null : evidence.syncRunId().value(),
					evidence.provenanceClass(),
					evidence.sourceDeviceOrApp(),
					evidence.qualityCode(),
					evidence.status(),
					evidence.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return IntegrationEvidencePersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<ConnectedEvidence> findByProviderAndAthleteIdAndExternalRecordId(
			HealthProviderKey provider,
			UUID athleteId,
			String externalRecordId) {
		return jpaRepository.findByProviderAndAthleteIdAndExternalRecordId(provider, athleteId, externalRecordId)
				.map(IntegrationEvidencePersistenceMapper::toDomain);
	}

	@Override
	public List<ConnectedEvidence> findByAthleteIdAndObservedAtBetween(
			UUID athleteId,
			Instant fromInclusive,
			Instant toInclusive,
			SignalFamily familyOrNull) {
		return jpaRepository.findActiveByAthleteAndWindow(athleteId, fromInclusive, toInclusive, familyOrNull).stream()
				.map(IntegrationEvidencePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<ConnectedEvidence> findByConnectionIdAndObservedAtBetween(
			ConnectionId connectionId,
			Instant fromInclusive,
			Instant toInclusive,
			SignalFamily familyOrNull) {
		return jpaRepository
				.findActiveByConnectionAndWindow(connectionId.value(), fromInclusive, toInclusive, familyOrNull)
				.stream()
				.map(IntegrationEvidencePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public long countByConnectionId(ConnectionId connectionId) {
		return jpaRepository.countByConnectionId(connectionId.value());
	}
}
