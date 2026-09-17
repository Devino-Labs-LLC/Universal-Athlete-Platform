package com.devinolabs.uap.billing.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

public interface ProviderEventInbox {

	/**
	 * Inserts a RECEIVED receipt. Empty when {@code (provider, providerEventId)} already exists.
	 */
	Optional<ProviderEventReceipt> tryBegin(
			BillingProvider provider,
			String providerEventId,
			String eventType,
			Instant receivedAt);

	Optional<ProviderEventReceipt> find(BillingProvider provider, String providerEventId);

	void complete(UUID id, ProviderEventProcessingStatus status, Instant processedAt);

}
