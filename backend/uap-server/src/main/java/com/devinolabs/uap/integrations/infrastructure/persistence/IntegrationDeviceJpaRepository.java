package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

interface IntegrationDeviceJpaRepository extends JpaRepository<IntegrationDeviceJpaEntity, UUID> {

	List<IntegrationDeviceJpaEntity> findByAccountIdOrderByLastSeenAtDesc(UUID accountId);

	List<IntegrationDeviceJpaEntity> findByAthleteIdOrderByLastSeenAtDesc(UUID athleteId);

	List<IntegrationDeviceJpaEntity> findByConnectionId(UUID connectionId);

	Optional<IntegrationDeviceJpaEntity> findByAthleteIdAndProviderAndExternalDeviceId(
			UUID athleteId,
			HealthProviderKey provider,
			String externalDeviceId);
}
