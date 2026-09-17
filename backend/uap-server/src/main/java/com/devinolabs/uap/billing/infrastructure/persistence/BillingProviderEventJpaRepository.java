package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.billing.domain.BillingProvider;

interface BillingProviderEventJpaRepository extends JpaRepository<BillingProviderEventJpaEntity, UUID> {

	Optional<BillingProviderEventJpaEntity> findByProviderAndProviderEventId(
			BillingProvider provider,
			String providerEventId);

}
