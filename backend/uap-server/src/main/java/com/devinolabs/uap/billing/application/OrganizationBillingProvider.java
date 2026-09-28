package com.devinolabs.uap.billing.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;

public interface OrganizationBillingProvider {

	String createCustomer(UUID organizationId);

	CheckoutSession createCheckoutSession(
			UUID organizationId,
			UUID subscriptionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence);

	ProviderSubscriptionSnapshot fetchCheckoutSubscription(
			UUID organizationId,
			UUID subscriptionId,
			String checkoutSessionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence);

	VerifiedProviderEvent verifyWebhook(byte[] payload, String signatureHeader);

	ProviderSubscriptionSnapshot fetchAuthoritativeSnapshot(VerifiedProviderEvent event);

	PortalSession createPortalSession(UUID organizationId, String providerCustomerRef);

	ProviderSubscriptionSnapshot changeSubscriptionPlan(
			UUID subscriptionId,
			String providerSubscriptionRef,
			CommercialPlanKey targetPlanKey,
			BillingCadence targetCadence,
			UUID requestId);

	ProviderSubscriptionSnapshot restoreSubscriptionPlan(
			UUID subscriptionId,
			String providerSubscriptionRef,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			String operationToken);

	ProviderSubscriptionSnapshot scheduleCancelAtPeriodEnd(
			UUID subscriptionId,
			String providerSubscriptionRef,
			UUID requestId);

	ProviderSubscriptionSnapshot reactivateSubscription(
			UUID subscriptionId,
			String providerSubscriptionRef,
			UUID requestId);

	ProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionRef);

	/**
	 * Cancels the same provider subscription immediately when it is still delinquent.
	 * Returns the current snapshot without cancelling when the provider is active, trialing, or already terminal.
	 */
	ProviderSubscriptionSnapshot terminateForNonpayment(
			UUID subscriptionId,
			String providerSubscriptionRef,
			String idempotencyKey);

	PendingCheckoutInspection lookupPendingCheckout(String providerCustomerRef, UUID subscriptionId);

	record PendingCheckoutInspection(Outcome outcome, ProviderSubscriptionSnapshot fulfilledSnapshot) {

		public enum Outcome {
			OPEN,
			EXPIRED,
			COMPLETE,
			NOT_FOUND
		}

		public PendingCheckoutInspection {
			Objects.requireNonNull(outcome, "outcome must not be null");
			if (outcome == Outcome.COMPLETE) {
				Objects.requireNonNull(fulfilledSnapshot, "fulfilledSnapshot must not be null");
			}
			else if (fulfilledSnapshot != null) {
				throw new IllegalArgumentException("fulfilledSnapshot is only present for COMPLETE");
			}
		}

		public static PendingCheckoutInspection of(Outcome outcome) {
			return new PendingCheckoutInspection(outcome, null);
		}
	}

	record PortalSession(String hostedUrl) {

		public PortalSession {
			Objects.requireNonNull(hostedUrl, "hostedUrl must not be null");
			hostedUrl = hostedUrl.trim();
			if (hostedUrl.isEmpty()) {
				throw new IllegalArgumentException("hostedUrl must not be blank");
			}
		}
	}

	record CheckoutSession(String sessionId, String checkoutUrl) {

		public CheckoutSession {
			sessionId = requireText(sessionId, "sessionId");
			checkoutUrl = requireText(checkoutUrl, "checkoutUrl");
		}

		private static String requireText(String value, String label) {
			Objects.requireNonNull(value, label + " must not be null");
			String normalized = value.trim();
			if (normalized.isEmpty()) {
				throw new IllegalArgumentException(label + " must not be blank");
			}
			return normalized;
		}
	}

	record VerifiedProviderEvent(
			String eventId,
			String eventType,
			boolean liveMode,
			Instant createdAt,
			String checkoutSessionId,
			String providerSubscriptionRef,
			UUID organizationId,
			UUID subscriptionId,
			UUID accountId) {

		public VerifiedProviderEvent {
			eventId = requireText(eventId, "eventId");
			eventType = requireText(eventType, "eventType");
			Objects.requireNonNull(createdAt, "createdAt must not be null");
			checkoutSessionId = blankToNull(checkoutSessionId);
			providerSubscriptionRef = blankToNull(providerSubscriptionRef);
		}

		private static String requireText(String value, String label) {
			Objects.requireNonNull(value, label + " must not be null");
			String normalized = value.trim();
			if (normalized.isEmpty()) {
				throw new IllegalArgumentException(label + " must not be blank");
			}
			return normalized;
		}

		private static String blankToNull(String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			return value.trim();
		}
	}

}
