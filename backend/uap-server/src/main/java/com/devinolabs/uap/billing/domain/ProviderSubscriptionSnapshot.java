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
		CommercialPlanKey planKey,
		BillingCadence billingCadence,
		Instant providerStateAsOf,
		ProviderCollectionState collectionState) {

	public ProviderSubscriptionSnapshot(
			String providerCustomerRef,
			String providerSubscriptionRef,
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			Instant providerStateAsOf) {
		this(
				providerCustomerRef,
				providerSubscriptionRef,
				status,
				cancelAtPeriodEnd,
				trialEndsAt,
				currentPeriodEndsAt,
				planKey,
				billingCadence,
				providerStateAsOf,
				ProviderCollectionState.NONE);
	}

	public ProviderSubscriptionSnapshot {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(planKey, "planKey must not be null");
		Objects.requireNonNull(billingCadence, "billingCadence must not be null");
		Objects.requireNonNull(providerStateAsOf, "providerStateAsOf must not be null");
		Objects.requireNonNull(collectionState, "collectionState must not be null");
		providerCustomerRef = normalize(providerCustomerRef);
		providerSubscriptionRef = normalize(providerSubscriptionRef);
		if (providerSubscriptionRef == null) {
			throw new IllegalArgumentException("providerSubscriptionRef must not be blank");
		}
		if (status == ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED) {
			if (collectionState == ProviderCollectionState.NONE) {
				throw new IllegalArgumentException("Payment attention requires a collection state");
			}
		}
		else if (collectionState != ProviderCollectionState.NONE) {
			throw new IllegalArgumentException("Collection state applies only to payment attention");
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
