package com.devinolabs.uap.billing.infrastructure.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;
import com.devinolabs.uap.billing.infrastructure.apple.AppleAppStoreServerClient.TransactionInfo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fixture-only App Store Server API client tests. No live Apple network calls.
 */
class HttpAppleAppStoreServerClientTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private AppleBillingProperties properties;
	private RestTemplate restTemplate;
	private PrivateKey privateKey;
	private HttpAppleAppStoreServerClient client;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		KeyPair keyPair = generator.generateKeyPair();
		privateKey = keyPair.getPrivate();

		properties = new AppleBillingProperties();
		properties.setEnabled(true);
		properties.setBundleId("com.devinolabs.athletereadiness");
		properties.setKeyId("KEYID");
		properties.setIssuerId("00000000-0000-0000-0000-000000000001");
		properties.setPrivateKeyPem(toPem(privateKey));
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");

		restTemplate = mock(RestTemplate.class);
		client = new HttpAppleAppStoreServerClient(properties, restTemplate, privateKey);
	}

	@Test
	void getTransactionInfoMapsSignedTransactionClaims() {
		String signedClaims = signedJwt("""
				{"transactionId":"2001","originalTransactionId":"1001","productId":"premium.monthly",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Sandbox",
				"appAccountToken":"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
				"purchaseDate":1727542800000,"expiresDate":1730134800000,"signedDate":1727542800000,
				"autoRenewStatus":1,"status":"1"}
				""");
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenReturn(ResponseEntity.ok("{\"signedTransactionInfo\":\"" + signedClaims + "\"}"));

		TransactionInfo info = client.getTransactionInfo("2001");

		assertThat(info.transactionId()).isEqualTo("2001");
		assertThat(info.originalTransactionId()).isEqualTo("1001");
		assertThat(info.productId()).isEqualTo("premium.monthly");
		assertThat(info.appAccountToken()).isEqualTo("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
		assertThat(info.autoRenewEnabled()).isTrue();
		assertThat(info.subscriptionStatus()).isEqualTo("1");
	}

	@Test
	void getTransactionInfoMaps404ToInvalidPurchase() {
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenThrow(HttpClientErrorException.create(
						HttpStatus.NOT_FOUND, "Not Found", null, null, StandardCharsets.UTF_8));

		assertThatThrownBy(() -> client.getTransactionInfo("missing"))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("not found");
	}

	@Test
	void getTransactionInfoMapsTransportFailureToProviderUnavailable() {
		when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
				.thenThrow(new RestClientException("boom"));

		assertThatThrownBy(() -> client.getTransactionInfo("2001"))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	@Test
	void blankTransactionIdFailsClosed() {
		assertThatThrownBy(() -> client.getTransactionInfo("  "))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("required");
	}

	@Test
	void incompleteClaimsFailClosed() {
		JsonNode claims = JSON.readTree("""
				{"transactionId":"2001","productId":"premium.monthly"}
				""");

		assertThatThrownBy(() -> client.mapClaims(claims))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void decodeJwtPayloadRejectsMalformedInput() {
		assertThatThrownBy(() -> HttpAppleAppStoreServerClient.decodeJwtPayload("not-a-jwt"))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("malformed");
	}

	@Test
	void extractTransactionIdRequiresClaim() {
		String signed = signedJwt("{\"productId\":\"premium.monthly\"}");

		assertThatThrownBy(() ->
						HttpAppleAppStoreServerClient.extractTransactionIdFromSignedTransaction(signed))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("transactionId");
	}

	@Test
	void parsePrivateKeyRejectsInvalidPem() {
		assertThatThrownBy(() -> new HttpAppleAppStoreServerClient(invalidKeyProperties()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("private-key-pem");
	}

	private static AppleBillingProperties invalidKeyProperties() {
		AppleBillingProperties properties = new AppleBillingProperties();
		properties.setEnabled(true);
		properties.setBundleId("com.devinolabs.athletereadiness");
		properties.setKeyId("KEYID");
		properties.setIssuerId("00000000-0000-0000-0000-000000000001");
		properties.setPrivateKeyPem("""
				-----BEGIN PRIVATE KEY-----
				not-valid-key-material
				-----END PRIVATE KEY-----
				""");
		properties.setEnvironment("Sandbox");
		return properties;
	}

	private static String toPem(PrivateKey privateKey) {
		String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
		return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
	}

	private static String signedJwt(String payloadJson) {
		String header = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		return header + "." + payload + ".sig";
	}

}
