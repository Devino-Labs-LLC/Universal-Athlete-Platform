package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

interface IntegrationOpsCounterJpaRepository extends JpaRepository<IntegrationOpsCounterJpaEntity, UUID> {

	Optional<IntegrationOpsCounterJpaEntity> findByProviderAndWindowStart(
			HealthProviderKey provider,
			Instant windowStart);
}
