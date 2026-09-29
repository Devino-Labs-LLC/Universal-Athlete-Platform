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
 * Apple App Store purchase / notification verification port.
 * Apple JWS types and HTTP clients stay behind the infrastructure adapter.
 */
public interface AppleAppStoreBillingProvider {

	VerifiedPurchase validateSignedTransaction(String signedTransactionInfo);

	VerifiedNotification verifySignedNotification(String signedPayload);

	ProviderSubscriptionSnapshot toSnapshot(VerifiedPurchase purchase, UUID accountId);

	record VerifiedPurchase(
			String originalTransactionId,
			String transactionId,
			String productId,
			String bundleId,
			String environment,
			String appAccountToken,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			Instant purchaseDate,
			Instant expiresDate,
			Instant providerStateAsOf,
			ProviderCollectionState collectionState) {

		public VerifiedPurchase {
			originalTransactionId = requireText(originalTransactionId, "originalTransactionId");
			transactionId = requireText(transactionId, "transactionId");
			productId = requireText(productId, "productId");
			bundleId = requireText(bundleId, "bundleId");
			environment = requireText(environment, "environment");
			appAccountToken = normalizeOptional(appAccountToken);
			Objects.requireNonNull(planKey, "planKey must not be null");
			Objects.requireNonNull(billingCadence, "billingCadence must not be null");
			Objects.requireNonNull(status, "status must not be null");
			Objects.requireNonNull(providerStateAsOf, "providerStateAsOf must not be null");
			Objects.requireNonNull(collectionState, "collectionState must not be null");
		}
	}

	record VerifiedNotification(
			String notificationUUID,
			String notificationType,
			String subtype,
			VerifiedPurchase purchase,
			Instant signedDate) {

		public VerifiedNotification {
			notificationUUID = requireText(notificationUUID, "notificationUUID");
			notificationType = requireText(notificationType, "notificationType");
			subtype = normalizeOptional(subtype);
			Objects.requireNonNull(purchase, "purchase must not be null");
			Objects.requireNonNull(signedDate, "signedDate must not be null");
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
