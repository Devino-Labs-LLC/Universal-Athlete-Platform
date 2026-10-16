package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.ProviderCertificationState;

@Entity
@Table(name = "integration_provider_certification")
class IntegrationProviderCertificationJpaEntity {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 40)
	private HealthProviderKey provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "certification_state", nullable = false, length = 20)
	private ProviderCertificationState certificationState;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IntegrationProviderCertificationJpaEntity() {
	}

	HealthProviderKey getProvider() {
		return provider;
	}

	ProviderCertificationState getCertificationState() {
		return certificationState;
	}
}
