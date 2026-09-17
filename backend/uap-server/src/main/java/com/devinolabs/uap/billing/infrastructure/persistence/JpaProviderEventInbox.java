package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.PersistenceException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.UnexpectedRollbackException;

import com.devinolabs.uap.billing.application.ProviderEventInbox;
import com.devinolabs.uap.billing.application.ProviderEventReceipt;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

@Repository
class JpaProviderEventInbox implements ProviderEventInbox {

	private final BillingProviderEventJpaRepository jpaRepository;
	private final ProviderEventClaimService claimService;

	JpaProviderEventInbox(
			BillingProviderEventJpaRepository jpaRepository,
			ProviderEventClaimService claimService) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
		this.claimService = Objects.requireNonNull(claimService);
	}

	@Override
	public Optional<ProviderEventReceipt> tryBegin(
			BillingProvider provider,
			String providerEventId,
			String eventType,
			Instant receivedAt) {
		Optional<ProviderEventReceipt> existing = lookup(provider, providerEventId);
		if (existing.isPresent()) {
			return retryable(existing.get());
		}
		try {
			return Optional.of(claimService.insertReceived(provider, providerEventId, eventType, receivedAt));
		}
		catch (DataIntegrityViolationException | PersistenceException | UnexpectedRollbackException ex) {
			return lookup(provider, providerEventId).flatMap(this::retryable);
		}
	}

	@Override
	public Optional<ProviderEventReceipt> find(BillingProvider provider, String providerEventId) {
		return lookup(provider, providerEventId);
	}

	@Override
	public void complete(UUID id, ProviderEventProcessingStatus status, Instant processedAt) {
		BillingProviderEventJpaEntity entity = jpaRepository.findById(id)
				.orElseThrow(() -> new IllegalStateException("Provider event receipt was not found"));
		entity.complete(status, processedAt);
		jpaRepository.save(entity);
	}

	private Optional<ProviderEventReceipt> lookup(BillingProvider provider, String providerEventId) {
		return jpaRepository.findByProviderAndProviderEventId(provider, providerEventId).map(this::toReceipt);
	}

	private Optional<ProviderEventReceipt> retryable(ProviderEventReceipt receipt) {
		if (receipt.processingStatus() == ProviderEventProcessingStatus.PROCESSED
				|| receipt.processingStatus() == ProviderEventProcessingStatus.IGNORED) {
			return Optional.empty();
		}
		return Optional.of(receipt);
	}

	private ProviderEventReceipt toReceipt(BillingProviderEventJpaEntity entity) {
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
