package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.SyncRunRepository;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

@Repository
class JpaSyncRunRepository implements SyncRunRepository {

	private final IntegrationSyncRunJpaRepository jpaRepository;

	JpaSyncRunRepository(IntegrationSyncRunJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public SyncRun save(SyncRun syncRun) {
		Optional<IntegrationSyncRunJpaEntity> existing = jpaRepository.findById(syncRun.id().value());
		IntegrationSyncRunJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(IntegrationSyncRunPersistenceMapper.toEntity(syncRun, true));
		}
		else {
			IntegrationSyncRunJpaEntity entity = existing.get();
			entity.applyDomainState(
					syncRun.status(),
					syncRun.startedAt(),
					syncRun.finishedAt(),
					syncRun.errorCode(),
					syncRun.recordsAccepted(),
					syncRun.recordsRejected());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return IntegrationSyncRunPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<SyncRun> findById(SyncRunId id) {
		return jpaRepository.findById(id.value()).map(IntegrationSyncRunPersistenceMapper::toDomain);
	}

	@Override
	public List<SyncRun> findByConnectionIdOrderByRequestedAtDesc(ConnectionId connectionId) {
		return jpaRepository.findAllByConnectionIdOrderByRequestedAtDesc(connectionId.value()).stream()
				.map(IntegrationSyncRunPersistenceMapper::toDomain)
				.toList();
	}

}
