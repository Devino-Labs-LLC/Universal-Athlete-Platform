package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.ProviderCertificationRepository;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.ProviderCertificationState;

@Repository
class JpaProviderCertificationRepository implements ProviderCertificationRepository {

	private final IntegrationProviderCertificationJpaRepository jpaRepository;

	JpaProviderCertificationRepository(IntegrationProviderCertificationJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Optional<ProviderCertificationState> findState(HealthProviderKey provider) {
		return jpaRepository.findById(provider).map(IntegrationProviderCertificationJpaEntity::getCertificationState);
	}
}
