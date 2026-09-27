package com.devinolabs.uap.billing.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

public interface ProviderEventInbox {

	/**
	 * Inserts a RECEIVED receipt, or returns an existing RECEIVED or FAILED receipt so Stripe
	 * redelivery can retry it. PROCESSED and IGNORED receipts return empty.
	 */
	Optional<ProviderEventReceipt> tryBegin(
			BillingProvider provider,
			String providerEventId,
			String eventType,
			Instant receivedAt);

	Optional<ProviderEventReceipt> find(BillingProvider provider, String providerEventId);

	void complete(UUID id, ProviderEventProcessingStatus status, Instant processedAt);

}
