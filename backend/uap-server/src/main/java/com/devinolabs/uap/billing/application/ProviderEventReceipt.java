package com.devinolabs.uap.billing.application;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

public record ProviderEventReceipt(
		UUID id,
		BillingProvider provider,
		String providerEventId,
		String eventType,
		Instant receivedAt,
		Instant processedAt,
		ProviderEventProcessingStatus processingStatus) {
}
