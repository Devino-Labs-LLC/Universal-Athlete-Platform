package com.devinolabs.uap.billing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Stripe customer identity owned by one Organization. */
public record OrganizationBillingCustomer(
		UUID organizationId,
		BillingProvider provider,
		String providerCustomerRef,
		Instant createdAt,
		Instant updatedAt) {

	public OrganizationBillingCustomer {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		providerCustomerRef = normalize(providerCustomerRef);
		if (provider != BillingProvider.STRIPE) {
			throw new IllegalArgumentException("Organization billing supports Stripe only");
		}
	}

	public static OrganizationBillingCustomer stripe(
			UUID organizationId,
			String providerCustomerRef,
			Instant now) {
		return new OrganizationBillingCustomer(
				organizationId,
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
