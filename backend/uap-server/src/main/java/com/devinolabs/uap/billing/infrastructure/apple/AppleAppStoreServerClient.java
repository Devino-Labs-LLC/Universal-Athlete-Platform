package com.devinolabs.uap.billing.infrastructure.apple;

import java.time.Instant;
import java.util.Objects;

/**
 * Authoritative App Store Server API transaction lookup. Kept behind the Apple adapter.
 */
interface AppleAppStoreServerClient {

	TransactionInfo getTransactionInfo(String transactionId);

	record TransactionInfo(
			String transactionId,
			String originalTransactionId,
			String productId,
			String bundleId,
			String environment,
			String appAccountToken,
			Instant purchaseDate,
			Instant expiresDate,
			Instant signedDate,
			boolean autoRenewEnabled,
			String subscriptionStatus) {

		public TransactionInfo {
			transactionId = requireText(transactionId, "transactionId");
			originalTransactionId = requireText(originalTransactionId, "originalTransactionId");
			productId = requireText(productId, "productId");
			bundleId = requireText(bundleId, "bundleId");
			environment = requireText(environment, "environment");
			appAccountToken = normalizeOptional(appAccountToken);
			Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
			Objects.requireNonNull(signedDate, "signedDate must not be null");
			subscriptionStatus = normalizeOptional(subscriptionStatus);
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
