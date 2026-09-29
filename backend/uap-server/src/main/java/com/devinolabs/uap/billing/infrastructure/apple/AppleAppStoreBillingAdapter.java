package com.devinolabs.uap.billing.infrastructure.apple;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider;
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.infrastructure.apple.AppleAppStoreServerClient.TransactionInfo;
import com.devinolabs.uap.billing.infrastructure.apple.AppleBillingProperties.PricedProduct;
import tools.jackson.databind.JsonNode;

/**
 * Apple App Store adapter. Keeps Apple JWS / Server API types out of the billing domain.
 */
@Component
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
class AppleAppStoreBillingAdapter implements AppleAppStoreBillingProvider {

	private static final Set<String> ACTIVE_STATUSES = Set.of("1", "ACTIVE", "active");
	private static final Set<String> EXPIRED_STATUSES = Set.of("2", "EXPIRED", "expired", "REVOKED", "revoked");
	private static final Set<String> BILLING_RETRY_STATUSES = Set.of("3", "BILLING_RETRY", "billing_retry");
	private static final Set<String> GRACE_STATUSES = Set.of("4", "BILLING_GRACE_PERIOD", "billing_grace_period");

	private final AppleBillingProperties properties;
	private final AppleAppStoreServerClient serverClient;

	AppleAppStoreBillingAdapter(AppleBillingProperties properties, AppleAppStoreServerClient serverClient) {
		this.properties = Objects.requireNonNull(properties);
		this.serverClient = Objects.requireNonNull(serverClient);
	}

	@Override
	public VerifiedPurchase validateSignedTransaction(String signedTransactionInfo) {
		rejectFakeToken(signedTransactionInfo);
		String transactionId = HttpAppleAppStoreServerClient.extractTransactionIdFromSignedTransaction(
				signedTransactionInfo.trim());
		TransactionInfo info = serverClient.getTransactionInfo(transactionId);
		return toVerifiedPurchase(info, null);
	}

	@Override
	public VerifiedNotification verifySignedNotification(String signedPayload) {
		rejectFakeToken(signedPayload);
		JsonNode outer = HttpAppleAppStoreServerClient.decodeJwtPayload(signedPayload.trim());
		String notificationUUID = HttpAppleAppStoreServerClient.text(outer, "notificationUUID");
		String notificationType = HttpAppleAppStoreServerClient.text(outer, "notificationType");
		if (notificationUUID == null || notificationType == null) {
			throw new InvalidApplePurchaseException("Apple notification claims were incomplete");
		}
		String subtype = HttpAppleAppStoreServerClient.text(outer, "subtype");
		Instant signedDate = Instant.now();
		JsonNode signedDateNode = outer.get("signedDate");
		if (signedDateNode != null && !signedDateNode.isNull()) {
			signedDate = Instant.ofEpochMilli(signedDateNode.asLong());
		}
		JsonNode data = outer.path("data");
		String signedTransactionInfo = HttpAppleAppStoreServerClient.text(data, "signedTransactionInfo");
		if (signedTransactionInfo == null) {
			throw new InvalidApplePurchaseException("Apple notification lacked signedTransactionInfo");
		}
		String transactionId = HttpAppleAppStoreServerClient.extractTransactionIdFromSignedTransaction(
				signedTransactionInfo);
		TransactionInfo info = serverClient.getTransactionInfo(transactionId);
		VerifiedPurchase purchase = toVerifiedPurchase(info, notificationType);
		return new VerifiedNotification(notificationUUID, notificationType, subtype, purchase, signedDate);
	}

	@Override
	public ProviderSubscriptionSnapshot toSnapshot(VerifiedPurchase purchase, UUID accountId) {
		Objects.requireNonNull(purchase, "purchase must not be null");
		Objects.requireNonNull(accountId, "accountId must not be null");
		String customerRef = purchase.appAccountToken() != null
				? purchase.appAccountToken()
				: accountId.toString();
		return new ProviderSubscriptionSnapshot(
				customerRef,
				purchase.originalTransactionId(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				null,
				purchase.expiresDate(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.providerStateAsOf(),
				purchase.collectionState());
	}

	private VerifiedPurchase toVerifiedPurchase(TransactionInfo info, String notificationType) {
		if (!properties.getBundleId().equals(info.bundleId())) {
			throw new InvalidApplePurchaseException("Apple transaction bundle did not match configuration");
		}
		if (!"Sandbox".equalsIgnoreCase(info.environment())
				&& !"XCODE".equalsIgnoreCase(info.environment())) {
			throw new InvalidApplePurchaseException("Apple Production transactions are not authorized in V4 G2");
		}
		PricedProduct priced;
		try {
			priced = properties.requirePlanForProduct(info.productId());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidApplePurchaseException("Apple product is not allow-listed", ex);
		}

		MappedStatus mapped = mapStatus(info, notificationType);
		return new VerifiedPurchase(
				info.originalTransactionId(),
				info.transactionId(),
				info.productId(),
				info.bundleId(),
				info.environment(),
				info.appAccountToken(),
				priced.planKey(),
				priced.cadence(),
				mapped.status(),
				mapped.cancelAtPeriodEnd(),
				info.purchaseDate(),
				info.expiresDate(),
				info.signedDate(),
				mapped.collectionState());
	}

	private static MappedStatus mapStatus(TransactionInfo info, String notificationType) {
		if (notificationType != null) {
			String type = notificationType.toUpperCase(Locale.ROOT);
			if ("EXPIRED".equals(type)
					|| "GRACE_PERIOD_EXPIRED".equals(type)
					|| "REFUND".equals(type)
					|| "REVOKE".equals(type)) {
				return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
			}
			if ("DID_FAIL_TO_RENEW".equals(type)) {
				return new MappedStatus(
						ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
						!info.autoRenewEnabled(),
						ProviderCollectionState.PAST_DUE);
			}
			if ("DID_CHANGE_RENEWAL_STATUS".equals(type) && !info.autoRenewEnabled()) {
				return new MappedStatus(ProviderCommercialStatus.ACTIVE, true, ProviderCollectionState.NONE);
			}
		}

		String status = info.subscriptionStatus();
		if (status != null) {
			String normalized = status.trim();
			if (EXPIRED_STATUSES.contains(normalized)) {
				return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
			}
			if (BILLING_RETRY_STATUSES.contains(normalized) || GRACE_STATUSES.contains(normalized)) {
				return new MappedStatus(
						ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
						!info.autoRenewEnabled(),
						ProviderCollectionState.PAST_DUE);
			}
			if (ACTIVE_STATUSES.contains(normalized)) {
				return new MappedStatus(
						ProviderCommercialStatus.ACTIVE,
						!info.autoRenewEnabled(),
						ProviderCollectionState.NONE);
			}
		}

		if (info.expiresDate() != null && info.expiresDate().isBefore(info.signedDate())) {
			return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
		}
		return new MappedStatus(
				ProviderCommercialStatus.ACTIVE,
				!info.autoRenewEnabled(),
				ProviderCollectionState.NONE);
	}

	private static void rejectFakeToken(String token) {
		if (token == null || token.isBlank()) {
			throw new InvalidApplePurchaseException("Apple signed payload is required");
		}
		String normalized = token.trim();
		String lower = normalized.toLowerCase(Locale.ROOT);
		if (lower.contains("fake")
				|| lower.equals("null")
				|| lower.equals("undefined")
				|| !normalized.contains(".")) {
			throw new InvalidApplePurchaseException("Apple signed payload was rejected");
		}
	}

	private record MappedStatus(
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			ProviderCollectionState collectionState) {
	}

}
