package com.devinolabs.uap.integrations.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

interface IntegrationProviderCertificationJpaRepository
		extends JpaRepository<IntegrationProviderCertificationJpaEntity, HealthProviderKey> {
}
