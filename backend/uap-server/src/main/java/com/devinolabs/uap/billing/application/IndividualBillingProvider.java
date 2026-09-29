package com.devinolabs.uap.billing.application;

import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;

/**
 * Account-owned Individual Premium checkout port. Keeps org-shaped methods off this surface.
 * Method names are distinct from {@link OrganizationBillingProvider} so one Stripe adapter can implement both.
 */
public interface IndividualBillingProvider {

	String createAccountCustomer(UUID accountId);

	CheckoutSession createAccountCheckoutSession(
			UUID accountId,
			UUID subscriptionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence);

	ProviderSubscriptionSnapshot fetchAccountCheckoutSubscription(
			UUID accountId,
			UUID subscriptionId,
			String checkoutSessionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence);

	PortalSession createAccountPortalSession(UUID accountId, String providerCustomerRef);

	ProviderSubscriptionSnapshot scheduleAccountCancelAtPeriodEnd(
			UUID subscriptionId,
			String providerSubscriptionRef,
			UUID requestId);

	ProviderSubscriptionSnapshot reactivateAccountSubscription(
			UUID subscriptionId,
			String providerSubscriptionRef,
			UUID requestId);

	ProviderSubscriptionSnapshot fetchAccountSubscription(String providerSubscriptionRef);

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

}
