package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.ConnectionRepository;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Repository
class JpaConnectionRepository implements ConnectionRepository {

	private final IntegrationConnectionJpaRepository jpaRepository;

	JpaConnectionRepository(IntegrationConnectionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Connection save(Connection connection) {
		Optional<IntegrationConnectionJpaEntity> existing = jpaRepository.findById(connection.id().value());
		IntegrationConnectionJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(IntegrationConnectionPersistenceMapper.toEntity(connection, true));
		}
		else {
			IntegrationConnectionJpaEntity entity = existing.get();
			entity.applyDomainState(
					connection.status(),
					connection.processConsentGranted(),
					connection.processConsentGrantedAt(),
					connection.scopesJson(),
					connection.connectedAt(),
					connection.disconnectedAt(),
					connection.lastSuccessfulSyncAt(),
					connection.lastAttemptedSyncAt(),
					connection.providerUserRef(),
					connection.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return IntegrationConnectionPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Connection> findById(ConnectionId id) {
		return jpaRepository.findById(id.value()).map(IntegrationConnectionPersistenceMapper::toDomain);
	}

	@Override
	public List<Connection> findByAccountId(UUID accountId) {
		return jpaRepository.findAllByAccountIdOrderByCreatedAtAsc(accountId).stream()
				.map(IntegrationConnectionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<Connection> findByAthleteId(UUID athleteId) {
		return jpaRepository.findAllByAthleteIdOrderByCreatedAtAsc(athleteId).stream()
				.map(IntegrationConnectionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<Connection> findByAthleteIdAndProvider(UUID athleteId, HealthProviderKey provider) {
		return jpaRepository.findByAthleteIdAndProvider(athleteId, provider)
				.map(IntegrationConnectionPersistenceMapper::toDomain);
	}

	@Override
	public long countByAccountId(UUID accountId) {
		return jpaRepository.countByAccountId(accountId);
	}

}
