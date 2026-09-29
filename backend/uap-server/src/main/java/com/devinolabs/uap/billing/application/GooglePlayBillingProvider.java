package com.devinolabs.uap.billing.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;

/**
 * Google Play purchase / RTDN verification port.
 * Play Developer API types and HTTP clients stay behind the infrastructure adapter.
 */
public interface GooglePlayBillingProvider {

	VerifiedPurchase validatePurchase(String purchaseToken, String productId);

	VerifiedNotification verifyRtdnPayload(byte[] payload);

	ProviderSubscriptionSnapshot toSnapshot(VerifiedPurchase purchase, UUID accountId);

	record VerifiedPurchase(
			String purchaseToken,
			String orderId,
			String productId,
			String packageName,
			boolean testPurchase,
			String obfuscatedExternalAccountId,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			Instant startTime,
			Instant expiryTime,
			Instant providerStateAsOf,
			ProviderCollectionState collectionState) {

		public VerifiedPurchase {
			purchaseToken = requireText(purchaseToken, "purchaseToken");
			orderId = normalizeOptional(orderId);
			productId = requireText(productId, "productId");
			packageName = requireText(packageName, "packageName");
			obfuscatedExternalAccountId = normalizeOptional(obfuscatedExternalAccountId);
			Objects.requireNonNull(planKey, "planKey must not be null");
			Objects.requireNonNull(billingCadence, "billingCadence must not be null");
			Objects.requireNonNull(status, "status must not be null");
			Objects.requireNonNull(providerStateAsOf, "providerStateAsOf must not be null");
			Objects.requireNonNull(collectionState, "collectionState must not be null");
		}
	}

	record VerifiedNotification(
			String eventId,
			String notificationType,
			VerifiedPurchase purchase,
			Instant eventTime) {

		public VerifiedNotification {
			eventId = requireText(eventId, "eventId");
			notificationType = requireText(notificationType, "notificationType");
			Objects.requireNonNull(purchase, "purchase must not be null");
			Objects.requireNonNull(eventTime, "eventTime must not be null");
		}
	}

	private static String requireText(String value, String label) {
		Objects.requireNonNull(value, label + " must not be null");
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(label + " must not be blank");
		}
		return normalized;
	}

	private static String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
