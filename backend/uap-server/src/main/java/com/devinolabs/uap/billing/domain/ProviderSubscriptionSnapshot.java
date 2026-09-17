package com.devinolabs.uap.billing.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Authoritative provider subscription snapshot used for webhook/sync (ADR-044).
 * Timestamps come from the provider — not a competing Athlete Readiness trial clock.
 */
public record ProviderSubscriptionSnapshot(
		String providerCustomerRef,
		String providerSubscriptionRef,
		ProviderCommercialStatus status,
		boolean cancelAtPeriodEnd,
		Instant trialEndsAt,
		Instant currentPeriodEndsAt,
		Instant providerStateAsOf) {

	public ProviderSubscriptionSnapshot {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(providerStateAsOf, "providerStateAsOf must not be null");
		providerCustomerRef = normalize(providerCustomerRef);
		providerSubscriptionRef = normalize(providerSubscriptionRef);
		if (providerSubscriptionRef == null) {
			throw new IllegalArgumentException("providerSubscriptionRef must not be blank");
		}
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
