package com.devinolabs.uap.billing.infrastructure.google;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.infrastructure.google.GooglePlayBillingProperties.PricedProduct;
import com.devinolabs.uap.billing.infrastructure.google.GooglePlayDeveloperApiClient.SubscriptionPurchase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Play adapter. Keeps Play Developer API / RTDN types out of the billing domain.
 */
@Component
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
class GooglePlayBillingAdapter implements GooglePlayBillingProvider {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final Set<String> ACTIVE_STATES = Set.of(
			"SUBSCRIPTION_STATE_ACTIVE",
			"SUBSCRIPTION_STATE_CANCELED");
	private static final Set<String> ENDED_STATES = Set.of(
			"SUBSCRIPTION_STATE_EXPIRED",
			"SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED");
	private static final Set<String> ATTENTION_STATES = Set.of(
			"SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
			"SUBSCRIPTION_STATE_ON_HOLD",
			"SUBSCRIPTION_STATE_PAUSED");

	/** RTDN subscriptionNotification.notificationType numeric codes. */
	static final int RTDN_RECOVERED = 1;
	static final int RTDN_RENEWED = 2;
	static final int RTDN_CANCELED = 3;
	static final int RTDN_PURCHASED = 4;
	static final int RTDN_ON_HOLD = 5;
	static final int RTDN_IN_GRACE_PERIOD = 6;
	static final int RTDN_RESTARTED = 7;
	static final int RTDN_REVOKED = 12;
	static final int RTDN_EXPIRED = 13;

	private final GooglePlayBillingProperties properties;
	private final GooglePlayDeveloperApiClient apiClient;

	GooglePlayBillingAdapter(GooglePlayBillingProperties properties, GooglePlayDeveloperApiClient apiClient) {
		this.properties = Objects.requireNonNull(properties);
		this.apiClient = Objects.requireNonNull(apiClient);
	}

	@Override
	public VerifiedPurchase validatePurchase(String purchaseToken, String productId) {
		rejectFakeToken(purchaseToken);
		if (productId == null || productId.isBlank()) {
			throw new InvalidGooglePlayPurchaseException("Google Play productId is required");
		}
		SubscriptionPurchase info = apiClient.getSubscriptionPurchase(purchaseToken.trim());
		if (!productId.trim().equals(info.productId())) {
			throw new InvalidGooglePlayPurchaseException("Google Play productId did not match purchase");
		}
		return toVerifiedPurchase(info, null);
	}

	@Override
	public VerifiedNotification verifyRtdnPayload(byte[] payload) {
		if (payload == null || payload.length == 0) {
			throw new InvalidGooglePlayPurchaseException("Google Play RTDN payload is required");
		}
		String raw = new String(payload, StandardCharsets.UTF_8).trim();
		rejectFakeToken(raw);
		try {
			JsonNode envelope = JSON.readTree(raw);
			JsonNode message = envelope.path("message");
			String messageId = text(message, "messageId");
			String dataB64 = text(message, "data");
			if (dataB64 == null) {
				throw new InvalidGooglePlayPurchaseException("Google Play RTDN lacked message.data");
			}
			JsonNode notification = JSON.readTree(Base64.getDecoder().decode(padBase64(dataB64)));
			String packageName = text(notification, "packageName");
			if (packageName == null || !properties.getPackageName().equals(packageName)) {
				throw new InvalidGooglePlayPurchaseException("Google Play RTDN package did not match configuration");
			}
			JsonNode subscriptionNotification = notification.path("subscriptionNotification");
			if (subscriptionNotification.isMissingNode() || subscriptionNotification.isNull()) {
				throw new InvalidGooglePlayPurchaseException("Google Play RTDN lacked subscriptionNotification");
			}
			int notificationTypeCode = subscriptionNotification.path("notificationType").asInt(-1);
			String purchaseToken = text(subscriptionNotification, "purchaseToken");
			String subscriptionId = text(subscriptionNotification, "subscriptionId");
			if (purchaseToken == null || notificationTypeCode < 0) {
				throw new InvalidGooglePlayPurchaseException("Google Play RTDN claims were incomplete");
			}
			rejectFakeToken(purchaseToken);
			Instant eventTime = Instant.now();
			String eventTimeMillis = text(notification, "eventTimeMillis");
			if (eventTimeMillis != null) {
				try {
					eventTime = Instant.ofEpochMilli(Long.parseLong(eventTimeMillis));
				}
				catch (NumberFormatException ex) {
					throw new InvalidGooglePlayPurchaseException("Google Play RTDN eventTimeMillis was invalid", ex);
				}
			}
			String eventId = messageId != null
					? messageId
					: eventTimeMillis + ":" + notificationTypeCode + ":" + purchaseToken;
			SubscriptionPurchase info = apiClient.getSubscriptionPurchase(purchaseToken);
			if (subscriptionId != null && !subscriptionId.equals(info.productId())) {
				throw new InvalidGooglePlayPurchaseException("Google Play RTDN productId did not match purchase");
			}
			String notificationType = mapRtdnType(notificationTypeCode);
			VerifiedPurchase purchase = toVerifiedPurchase(info, notificationType);
			return new VerifiedNotification(eventId, notificationType, purchase, eventTime);
		}
		catch (InvalidGooglePlayPurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidGooglePlayPurchaseException("Google Play RTDN could not be verified", ex);
		}
	}

	@Override
	public ProviderSubscriptionSnapshot toSnapshot(VerifiedPurchase purchase, UUID accountId) {
		Objects.requireNonNull(purchase, "purchase must not be null");
		Objects.requireNonNull(accountId, "accountId must not be null");
		String customerRef = purchase.obfuscatedExternalAccountId() != null
				? purchase.obfuscatedExternalAccountId()
				: accountId.toString();
		return new ProviderSubscriptionSnapshot(
				customerRef,
				purchase.purchaseToken(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				null,
				purchase.expiryTime(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.providerStateAsOf(),
				purchase.collectionState());
	}

	private VerifiedPurchase toVerifiedPurchase(SubscriptionPurchase info, String notificationType) {
		if (!properties.getPackageName().equals(info.packageName())) {
			throw new InvalidGooglePlayPurchaseException("Google Play package did not match configuration");
		}
		if (!info.testPurchase()) {
			throw new InvalidGooglePlayPurchaseException(
					"Google Play Production purchases are not authorized in V4 G3");
		}
		PricedProduct priced;
		try {
			priced = properties.requirePlanForProduct(info.productId());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidGooglePlayPurchaseException("Google Play product is not allow-listed", ex);
		}

		MappedStatus mapped = mapStatus(info, notificationType);
		return new VerifiedPurchase(
				info.purchaseToken(),
				info.orderId(),
				info.productId(),
				info.packageName(),
				info.testPurchase(),
				info.obfuscatedExternalAccountId(),
				priced.planKey(),
				priced.cadence(),
				mapped.status(),
				mapped.cancelAtPeriodEnd(),
				info.startTime(),
				info.expiryTime(),
				info.providerStateAsOf(),
				mapped.collectionState());
	}

	private static MappedStatus mapStatus(SubscriptionPurchase info, String notificationType) {
		if (notificationType != null) {
			String type = notificationType.toUpperCase(Locale.ROOT);
			if ("SUBSCRIPTION_EXPIRED".equals(type) || "SUBSCRIPTION_REVOKED".equals(type)) {
				return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
			}
			if ("SUBSCRIPTION_ON_HOLD".equals(type) || "SUBSCRIPTION_IN_GRACE_PERIOD".equals(type)) {
				return new MappedStatus(
						ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
						!info.autoRenewEnabled(),
						ProviderCollectionState.PAST_DUE);
			}
			if ("SUBSCRIPTION_CANCELED".equals(type)) {
				return new MappedStatus(ProviderCommercialStatus.ACTIVE, true, ProviderCollectionState.NONE);
			}
		}

		String state = info.subscriptionState();
		if (state != null) {
			String normalized = state.trim();
			if (ENDED_STATES.contains(normalized)) {
				return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
			}
			if (ATTENTION_STATES.contains(normalized)) {
				return new MappedStatus(
						ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
						!info.autoRenewEnabled(),
						ProviderCollectionState.PAST_DUE);
			}
			if (ACTIVE_STATES.contains(normalized)) {
				boolean cancelAtPeriodEnd = "SUBSCRIPTION_STATE_CANCELED".equals(normalized)
						|| !info.autoRenewEnabled();
				return new MappedStatus(
						ProviderCommercialStatus.ACTIVE,
						cancelAtPeriodEnd,
						ProviderCollectionState.NONE);
			}
		}

		if (info.expiryTime() != null && info.expiryTime().isBefore(info.providerStateAsOf())) {
			return new MappedStatus(ProviderCommercialStatus.ENDED, false, ProviderCollectionState.NONE);
		}
		return new MappedStatus(
				ProviderCommercialStatus.ACTIVE,
				!info.autoRenewEnabled(),
				ProviderCollectionState.NONE);
	}

	static String mapRtdnType(int code) {
		return switch (code) {
			case RTDN_RECOVERED -> "SUBSCRIPTION_RECOVERED";
			case RTDN_RENEWED -> "SUBSCRIPTION_RENEWED";
			case RTDN_CANCELED -> "SUBSCRIPTION_CANCELED";
			case RTDN_PURCHASED -> "SUBSCRIPTION_PURCHASED";
			case RTDN_ON_HOLD -> "SUBSCRIPTION_ON_HOLD";
			case RTDN_IN_GRACE_PERIOD -> "SUBSCRIPTION_IN_GRACE_PERIOD";
			case RTDN_RESTARTED -> "SUBSCRIPTION_RESTARTED";
			case RTDN_REVOKED -> "SUBSCRIPTION_REVOKED";
			case RTDN_EXPIRED -> "SUBSCRIPTION_EXPIRED";
			default -> "SUBSCRIPTION_OTHER_" + code;
		};
	}

	private static void rejectFakeToken(String token) {
		if (token == null || token.isBlank()) {
			throw new InvalidGooglePlayPurchaseException("Google Play purchase token is required");
		}
		String normalized = token.trim();
		String lower = normalized.toLowerCase(Locale.ROOT);
		if (lower.contains("fake")
				|| lower.equals("null")
				|| lower.equals("undefined")
				|| normalized.length() < 20) {
			throw new InvalidGooglePlayPurchaseException("Google Play purchase token was rejected");
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static String padBase64(String value) {
		int mod = value.length() % 4;
		if (mod == 0) {
			return value;
		}
		return value + "====".substring(mod);
	}

	private record MappedStatus(
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			ProviderCollectionState collectionState) {
	}

}
