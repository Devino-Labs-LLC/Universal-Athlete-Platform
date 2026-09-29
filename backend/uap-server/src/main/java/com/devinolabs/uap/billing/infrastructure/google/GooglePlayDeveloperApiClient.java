package com.devinolabs.uap.billing.infrastructure.google;

import java.time.Instant;
import java.util.Objects;

/**
 * Authoritative Google Play Developer API subscription lookup. Kept behind the Google adapter.
 */
interface GooglePlayDeveloperApiClient {

	SubscriptionPurchase getSubscriptionPurchase(String purchaseToken);

	record SubscriptionPurchase(
			String purchaseToken,
			String orderId,
			String productId,
			String packageName,
			boolean testPurchase,
			String obfuscatedExternalAccountId,
			Instant startTime,
			Instant expiryTime,
			Instant providerStateAsOf,
			boolean autoRenewEnabled,
			String subscriptionState) {

		public SubscriptionPurchase {
			purchaseToken = requireText(purchaseToken, "purchaseToken");
			orderId = normalizeOptional(orderId);
			productId = requireText(productId, "productId");
			packageName = requireText(packageName, "packageName");
			obfuscatedExternalAccountId = normalizeOptional(obfuscatedExternalAccountId);
			Objects.requireNonNull(startTime, "startTime must not be null");
			Objects.requireNonNull(providerStateAsOf, "providerStateAsOf must not be null");
			subscriptionState = normalizeOptional(subscriptionState);
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

}
