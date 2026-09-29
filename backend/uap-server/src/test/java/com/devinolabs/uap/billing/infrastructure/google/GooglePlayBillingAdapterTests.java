package com.devinolabs.uap.billing.infrastructure.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.infrastructure.google.GooglePlayDeveloperApiClient.SubscriptionPurchase;

/**
 * Fixture-only adapter tests. No live Google Play Developer API calls.
 */
class GooglePlayBillingAdapterTests {

	private static final Instant NOW = Instant.parse("2026-09-28T18:00:00Z");
	private static final String TOKEN = "google-play-purchase-token-fixture-001";

	private GooglePlayBillingProperties properties;
	private GooglePlayDeveloperApiClient apiClient;
	private GooglePlayBillingAdapter adapter;

	@BeforeEach
	void setUp() {
		properties = new GooglePlayBillingProperties();
		properties.setEnabled(true);
		properties.setPackageName("com.devinolabs.athletereadiness");
		properties.setServiceAccountJson("""
				{"type":"service_account","client_email":"play@test.iam.gserviceaccount.com",
				"private_key":"-----BEGIN PRIVATE KEY-----\\nTEST\\n-----END PRIVATE KEY-----\\n"}
				""");
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");
		apiClient = mock(GooglePlayDeveloperApiClient.class);
		adapter = new GooglePlayBillingAdapter(properties, apiClient);
	}

	@Test
	void fakeTokenIsRejectedWithoutCallingGoogle() {
		assertThatThrownBy(() -> adapter.validatePurchase("fake-token", "premium.monthly"))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class);
	}

	@Test
	void fixturePurchaseMapsToVerifiedPurchase() {
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.1111-2222",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW.minusSeconds(60),
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));

		VerifiedPurchase purchase = adapter.validatePurchase(TOKEN, "premium.monthly");

		assertThat(purchase.planKey()).isEqualTo(CommercialPlanKey.INDIVIDUAL_PREMIUM);
		assertThat(purchase.billingCadence()).isEqualTo(BillingCadence.MONTHLY);
		assertThat(purchase.status()).isEqualTo(ProviderCommercialStatus.ACTIVE);
		assertThat(purchase.purchaseToken()).isEqualTo(TOKEN);
		assertThat(purchase.testPurchase()).isTrue();
	}

	@Test
	void unknownProductFailsClosed() {
		when(apiClient.getSubscriptionPurchase(anyString())).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.1111-2222",
				"unknown.product",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));

		assertThatThrownBy(() -> adapter.validatePurchase(TOKEN, "unknown.product"))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("allow-listed");
	}

	@Test
	void productionPurchaseWithoutTestMarkerFailsClosed() {
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.1111-2222",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				false,
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));

		assertThatThrownBy(() -> adapter.validatePurchase(TOKEN, "premium.monthly"))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("Production");
	}

	@Test
	void toSnapshotUsesAccountIdAsCustomerRefWhenObfuscatedIdAbsent() {
		UUID accountId = UUID.randomUUID();
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.3333-4444",
				"premium.annual",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW,
				NOW.plusSeconds(365 * 24 * 60 * 60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));
		VerifiedPurchase purchase = adapter.validatePurchase(TOKEN, "premium.annual");

		assertThat(adapter.toSnapshot(purchase, accountId).providerCustomerRef())
				.isEqualTo(accountId.toString());
		assertThat(adapter.toSnapshot(purchase, accountId).billingCadence())
				.isEqualTo(BillingCadence.ANNUAL);
	}

	@Test
	void fixtureRtdnEnvelopeVerifiesViaAuthoritativeLookup() {
		String developerNotification = """
				{"version":"1.0","packageName":"com.devinolabs.athletereadiness","eventTimeMillis":"1727542800000",
				"subscriptionNotification":{"version":"1.0","notificationType":2,
				"purchaseToken":"%s","subscriptionId":"premium.monthly"}}
				""".formatted(TOKEN);
		String data = Base64.getEncoder().encodeToString(developerNotification.getBytes(StandardCharsets.UTF_8));
		String envelope = """
				{"message":{"data":"%s","messageId":"msg-1","publishTime":"2026-09-28T18:00:00Z"},
				"subscription":"projects/test/subscriptions/play-rtdn"}
				""".formatted(data);
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.5555-6666",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW,
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));

		var notification = adapter.verifyRtdnPayload(envelope.getBytes(StandardCharsets.UTF_8));

		assertThat(notification.eventId()).isEqualTo("msg-1");
		assertThat(notification.notificationType()).isEqualTo("SUBSCRIPTION_RENEWED");
		assertThat(notification.purchase().purchaseToken()).isEqualTo(TOKEN);
	}

	@Test
	void productIdMismatchFailsClosed() {
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.1111-2222",
				"premium.annual",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"SUBSCRIPTION_STATE_ACTIVE"));

		assertThatThrownBy(() -> adapter.validatePurchase(TOKEN, "premium.monthly"))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("productId did not match");
	}

	@Test
	void gracePeriodStateMapsToPaymentAttention() {
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.7777-8888",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW.minusSeconds(60),
				NOW.plusSeconds(3 * 24 * 60 * 60),
				NOW,
				false,
				"SUBSCRIPTION_STATE_IN_GRACE_PERIOD"));

		VerifiedPurchase purchase = adapter.validatePurchase(TOKEN, "premium.monthly");

		assertThat(purchase.status()).isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
		assertThat(purchase.cancelAtPeriodEnd()).isTrue();
	}

	@Test
	void rtdnExpiredTypeMapsToEnded() {
		String developerNotification = """
				{"version":"1.0","packageName":"com.devinolabs.athletereadiness","eventTimeMillis":"1727542800000",
				"subscriptionNotification":{"version":"1.0","notificationType":13,
				"purchaseToken":"%s","subscriptionId":"premium.monthly"}}
				""".formatted(TOKEN);
		String data = Base64.getEncoder().encodeToString(developerNotification.getBytes(StandardCharsets.UTF_8));
		String envelope = """
				{"message":{"data":"%s","messageId":"msg-expired"},
				"subscription":"projects/test/subscriptions/play-rtdn"}
				""".formatted(data);
		when(apiClient.getSubscriptionPurchase(TOKEN)).thenReturn(new SubscriptionPurchase(
				TOKEN,
				"GPA.9999-0000",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				true,
				null,
				NOW.minusSeconds(120),
				NOW.minusSeconds(30),
				NOW,
				false,
				"SUBSCRIPTION_STATE_ACTIVE"));

		assertThat(adapter.verifyRtdnPayload(envelope.getBytes(StandardCharsets.UTF_8)).purchase().status())
				.isEqualTo(ProviderCommercialStatus.ENDED);
	}

	@Test
	void rtdnPackageMismatchFailsClosed() {
		String developerNotification = """
				{"version":"1.0","packageName":"com.other.app","eventTimeMillis":"1727542800000",
				"subscriptionNotification":{"version":"1.0","notificationType":2,
				"purchaseToken":"%s","subscriptionId":"premium.monthly"}}
				""".formatted(TOKEN);
		String data = Base64.getEncoder().encodeToString(developerNotification.getBytes(StandardCharsets.UTF_8));
		String envelope = """
				{"message":{"data":"%s","messageId":"msg-bad-pkg"},
				"subscription":"projects/test/subscriptions/play-rtdn"}
				""".formatted(data);

		assertThatThrownBy(() -> adapter.verifyRtdnPayload(envelope.getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("package");
	}

	@Test
	void mapRtdnTypeCoversKnownCodes() {
		assertThat(GooglePlayBillingAdapter.mapRtdnType(GooglePlayBillingAdapter.RTDN_CANCELED))
				.isEqualTo("SUBSCRIPTION_CANCELED");
		assertThat(GooglePlayBillingAdapter.mapRtdnType(GooglePlayBillingAdapter.RTDN_ON_HOLD))
				.isEqualTo("SUBSCRIPTION_ON_HOLD");
		assertThat(GooglePlayBillingAdapter.mapRtdnType(99)).isEqualTo("SUBSCRIPTION_OTHER_99");
	}

}
