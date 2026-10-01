package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

interface IntegrationConnectionJpaRepository extends JpaRepository<IntegrationConnectionJpaEntity, UUID> {

	List<IntegrationConnectionJpaEntity> findAllByAccountIdOrderByCreatedAtAsc(UUID accountId);

	List<IntegrationConnectionJpaEntity> findAllByAthleteIdOrderByCreatedAtAsc(UUID athleteId);

	Optional<IntegrationConnectionJpaEntity> findByAthleteIdAndProvider(UUID athleteId, HealthProviderKey provider);

	long countByAccountId(UUID accountId);

}
