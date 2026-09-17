package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.application.ProviderEventReceipt;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

/**
 * Commits a provider-event receipt in its own transaction so a unique-key collision
 * cannot mark the surrounding webhook transaction rollback-only.
 */
@Service
class ProviderEventClaimService {

	private final BillingProviderEventJpaRepository jpaRepository;

	ProviderEventClaimService(BillingProviderEventJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	ProviderEventReceipt insertReceived(
			BillingProvider provider,
			String providerEventId,
			String eventType,
			Instant receivedAt) {
		BillingProviderEventJpaEntity entity = new BillingProviderEventJpaEntity(
				UUID.randomUUID(),
				provider,
				providerEventId,
				eventType,
				receivedAt,
				null,
				ProviderEventProcessingStatus.RECEIVED,
				true);
		jpaRepository.saveAndFlush(entity);
		return toReceipt(entity);
	}

	private static ProviderEventReceipt toReceipt(BillingProviderEventJpaEntity entity) {
		return new ProviderEventReceipt(
				entity.getId(),
				entity.getProvider(),
				entity.getProviderEventId(),
				entity.getEventType(),
				entity.getReceivedAt(),
				entity.getProcessedAt(),
				entity.getProcessingStatus());
	}

}
