package com.devinolabs.uap.billing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Stripe customer identity owned by one Account. */
public record AccountBillingCustomer(
		UUID accountId,
		BillingProvider provider,
		String providerCustomerRef,
		Instant createdAt,
		Instant updatedAt) {

	public AccountBillingCustomer {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		providerCustomerRef = normalize(providerCustomerRef);
		if (provider != BillingProvider.STRIPE) {
			throw new IllegalArgumentException("Account billing supports Stripe only");
		}
	}

	public static AccountBillingCustomer stripe(
			UUID accountId,
			String providerCustomerRef,
			Instant now) {
		return new AccountBillingCustomer(
				accountId,
				BillingProvider.STRIPE,
				providerCustomerRef,
				now,
				now);
	}

	private static String normalize(String value) {
		Objects.requireNonNull(value, "providerCustomerRef must not be null");
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("providerCustomerRef must not be blank");
		}
		return normalized;
	}

}
