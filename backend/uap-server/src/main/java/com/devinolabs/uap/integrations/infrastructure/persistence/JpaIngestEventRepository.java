package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.IngestEventRepository;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.IngestEvent;
import com.devinolabs.uap.integrations.domain.IngestEventId;

@Repository
class JpaIngestEventRepository implements IngestEventRepository {

	private final IntegrationIngestEventJpaRepository jpaRepository;

	JpaIngestEventRepository(IntegrationIngestEventJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public IngestEvent save(IngestEvent event) {
		Optional<IntegrationIngestEventJpaEntity> existing = jpaRepository.findById(event.id().value());
		IntegrationIngestEventJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(IntegrationIngestEventPersistenceMapper.toEntity(event, true));
		}
		else {
			IntegrationIngestEventJpaEntity entity = existing.get();
			entity.applyDomainState(
					event.processedAt(),
					event.processingStatus(),
					event.errorCode(),
					event.syncRunId() == null ? null : event.syncRunId().value());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return IntegrationIngestEventPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<IngestEvent> findById(IngestEventId id) {
		return jpaRepository.findById(id.value()).map(IntegrationIngestEventPersistenceMapper::toDomain);
	}

	@Override
	public Optional<IngestEvent> findByProviderAndProviderEventId(HealthProviderKey provider, String providerEventId) {
		return jpaRepository.findByProviderAndProviderEventId(provider, providerEventId)
				.map(IntegrationIngestEventPersistenceMapper::toDomain);
	}
}
