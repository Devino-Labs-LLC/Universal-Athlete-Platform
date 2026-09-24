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
			UUID subscriptionId) {

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
