package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

interface IntegrationIngestEventJpaRepository extends JpaRepository<IntegrationIngestEventJpaEntity, UUID> {

	Optional<IntegrationIngestEventJpaEntity> findByProviderAndProviderEventId(
			HealthProviderKey provider,
			String providerEventId);
}
