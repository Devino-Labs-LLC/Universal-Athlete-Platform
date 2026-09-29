package com.devinolabs.uap.billing.infrastructure.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;
import com.devinolabs.uap.billing.infrastructure.google.GooglePlayDeveloperApiClient.SubscriptionPurchase;

/**
 * Fixture-only Google Play Developer API client tests. No live Google network calls.
 */
class HttpGooglePlayDeveloperApiClientTests {

	private static final String TOKEN = "google-play-purchase-token-fixture-abc123";

	private GooglePlayBillingProperties properties;
	private RestTemplate restTemplate;
	private PrivateKey privateKey;
	private HttpGooglePlayDeveloperApiClient client;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		privateKey = keyPair.getPrivate();

		properties = new GooglePlayBillingProperties();
		properties.setEnabled(true);
		properties.setPackageName("com.devinolabs.athletereadiness");
		properties.setServiceAccountJson("""
				{"type":"service_account","client_email":"play@test.iam.gserviceaccount.com",
				"private_key":"%s"}
				""".formatted(toPem(privateKey).replace("\n", "\\n")));
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");
		properties.validateWhenEnabled();

		restTemplate = mock(RestTemplate.class);
		client = new HttpGooglePlayDeveloperApiClient(properties, restTemplate, privateKey);
	}

	@Test
	void getSubscriptionPurchaseMapsActiveSandboxPurchase() {
		stubTokenThenPurchase(subscriptionBody(true));

		SubscriptionPurchase purchase = client.getSubscriptionPurchase(TOKEN);

		assertThat(purchase.purchaseToken()).isEqualTo(TOKEN);
		assertThat(purchase.productId()).isEqualTo("premium.monthly");
		assertThat(purchase.testPurchase()).isTrue();
		assertThat(purchase.obfuscatedExternalAccountId())
				.isEqualTo("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
		assertThat(purchase.subscriptionState()).isEqualTo("SUBSCRIPTION_STATE_ACTIVE");
		assertThat(purchase.autoRenewEnabled()).isTrue();
	}

	@Test
	void getSubscriptionPurchaseCachesAccessTokenAcrossCalls() {
		stubTokenThenPurchase(subscriptionBody(true));
		client.getSubscriptionPurchase(TOKEN);
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenReturn(ResponseEntity.ok(subscriptionBody(true)));

		client.getSubscriptionPurchase(TOKEN);

		verify(restTemplate, times(3)).exchange(any(RequestEntity.class), eq(String.class));
	}

	@Test
	void getSubscriptionPurchaseMaps404ToInvalidPurchase() {
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenReturn(ResponseEntity.ok("{\"access_token\":\"tok\",\"expires_in\":3600}"))
				.thenThrow(HttpClientErrorException.create(
						HttpStatus.NOT_FOUND, "Not Found", null, null, StandardCharsets.UTF_8));

		assertThatThrownBy(() -> client.getSubscriptionPurchase(TOKEN))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("not found");
	}

	@Test
	void getSubscriptionPurchaseMapsTransportFailureToProviderUnavailable() {
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenThrow(new RestClientException("boom"));

		assertThatThrownBy(() -> client.getSubscriptionPurchase(TOKEN))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	@Test
	void blankPurchaseTokenFailsClosed() {
		assertThatThrownBy(() -> client.getSubscriptionPurchase("  "))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("required");
	}

	@Test
	void incompleteLineItemsFailClosed() {
		assertThatThrownBy(() -> client.parseSubscriptionResponse(TOKEN, "{\"subscriptionState\":\"ACTIVE\"}"))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void invalidTimestampFailsClosed() {
		String body = """
				{"subscriptionState":"SUBSCRIPTION_STATE_ACTIVE","latestOrderId":"GPA.1",
				"startTime":"not-an-instant","lineItems":[{"productId":"premium.monthly"}]}
				""";

		assertThatThrownBy(() -> client.parseSubscriptionResponse(TOKEN, body))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("timestamp");
	}

	@Test
	void parsePrivateKeyRejectsInvalidPem() {
		GooglePlayBillingProperties broken = new GooglePlayBillingProperties();
		broken.setEnabled(true);
		broken.setPackageName("com.devinolabs.athletereadiness");
		broken.setServiceAccountJson("""
				{"type":"service_account","client_email":"play@test.iam.gserviceaccount.com",
				"private_key":"-----BEGIN PRIVATE KEY-----\\nbad\\n-----END PRIVATE KEY-----\\n"}
				""");

		assertThatThrownBy(() -> new HttpGooglePlayDeveloperApiClient(broken))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("private_key");
	}

	@SuppressWarnings("unchecked")
	private void stubTokenThenPurchase(String purchaseBody) {
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenReturn(ResponseEntity.ok("{\"access_token\":\"tok\",\"expires_in\":3600}"))
				.thenReturn(ResponseEntity.ok(purchaseBody));
	}

	private static String subscriptionBody(boolean testPurchase) {
		return """
				{"subscriptionState":"SUBSCRIPTION_STATE_ACTIVE","latestOrderId":"GPA.1111-2222",
				"startTime":"2026-09-01T00:00:00Z","testPurchase":%s,
				"externalAccountIdentifiers":{"obfuscatedExternalAccountId":"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"},
				"lineItems":[{"productId":"premium.monthly","expiryTime":"2026-10-01T00:00:00Z",
				"autoRenewingPlan":{"autoRenewEnabled":true}}]}
				""".formatted(testPurchase ? "{}" : "null");
	}

	private static String toPem(PrivateKey privateKey) {
		String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
		return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
	}

}
