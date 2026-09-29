package com.devinolabs.uap.billing.infrastructure.apple;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * App Store Server API client (Sandbox). Never logs the private key or bearer token.
 */
@Component
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
class HttpAppleAppStoreServerClient implements AppleAppStoreServerClient {

	private static final String SANDBOX_BASE = "https://api.storekit-sandbox.itunes.apple.com";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final AppleBillingProperties properties;
	private final RestTemplate restTemplate;
	private final PrivateKey privateKey;

	HttpAppleAppStoreServerClient(AppleBillingProperties properties) {
		this(properties, new RestTemplate(), parsePrivateKey(properties.getPrivateKeyPem()));
	}

	HttpAppleAppStoreServerClient(
			AppleBillingProperties properties,
			RestTemplate restTemplate,
			PrivateKey privateKey) {
		this.properties = Objects.requireNonNull(properties);
		this.restTemplate = Objects.requireNonNull(restTemplate);
		this.privateKey = Objects.requireNonNull(privateKey);
	}

	@Override
	public TransactionInfo getTransactionInfo(String transactionId) {
		String normalized = requireTransactionId(transactionId);
		String url = SANDBOX_BASE + "/inApps/v1/transactions/" + normalized;
		try {
			RequestEntity<Void> request = RequestEntity.get(url)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + createBearerToken())
					.accept(MediaType.APPLICATION_JSON)
					.build();
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new InvalidApplePurchaseException("Apple transaction was not found");
			}
			return parseTransactionResponse(response.getBody());
		}
		catch (InvalidApplePurchaseException ex) {
			throw ex;
		}
		catch (HttpStatusCodeException ex) {
			if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 400) {
				throw new InvalidApplePurchaseException("Apple transaction was not found");
			}
			throw new BillingProviderUnavailableException(ex);
		}
		catch (RestClientException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	private TransactionInfo parseTransactionResponse(String body) {
		try {
			JsonNode root = JSON.readTree(body);
			String signedTransactionInfo = text(root, "signedTransactionInfo");
			if (signedTransactionInfo == null) {
				throw new InvalidApplePurchaseException("Apple transaction payload was incomplete");
			}
			JsonNode claims = decodeJwtPayload(signedTransactionInfo);
			return mapClaims(claims);
		}
		catch (InvalidApplePurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidApplePurchaseException("Apple transaction payload was invalid", ex);
		}
	}

	TransactionInfo mapClaims(JsonNode claims) {
		String transactionId = text(claims, "transactionId");
		String originalTransactionId = text(claims, "originalTransactionId");
		String productId = text(claims, "productId");
		String bundleId = text(claims, "bundleId");
		String environment = text(claims, "environment");
		if (transactionId == null || originalTransactionId == null || productId == null
				|| bundleId == null || environment == null) {
			throw new InvalidApplePurchaseException("Apple transaction claims were incomplete");
		}
		Instant purchaseDate = epochMillis(claims, "purchaseDate");
		Instant expiresDate = optionalEpochMillis(claims, "expiresDate");
		Instant signedDate = optionalEpochMillis(claims, "signedDate");
		if (signedDate == null) {
			signedDate = purchaseDate;
		}
		boolean autoRenewEnabled = !claims.has("autoRenewStatus")
				|| claims.path("autoRenewStatus").asInt(1) == 1;
		String status = text(claims, "status");
		return new TransactionInfo(
				transactionId,
				originalTransactionId,
				productId,
				bundleId,
				environment,
				text(claims, "appAccountToken"),
				purchaseDate,
				expiresDate,
				signedDate,
				autoRenewEnabled,
				status);
	}

	private String createBearerToken() {
		try {
			long now = Instant.now().getEpochSecond();
			String headerJson = "{\"alg\":\"ES256\",\"kid\":\"" + escape(properties.getKeyId()) + "\",\"typ\":\"JWT\"}";
			String payloadJson = "{"
					+ "\"iss\":\"" + escape(properties.getIssuerId()) + "\","
					+ "\"iat\":" + now + ","
					+ "\"exp\":" + (now + 1200) + ","
					+ "\"aud\":\"appstoreconnect-v1\","
					+ "\"bid\":\"" + escape(properties.getBundleId()) + "\","
					+ "\"nonce\":\"" + UUID.randomUUID() + "\""
					+ "}";
			String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
			String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
			String signingInput = header + "." + payload;
			Signature signature = Signature.getInstance("SHA256withECDSA");
			signature.initSign(privateKey);
			signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
			String sig = base64Url(signature.sign());
			return signingInput + "." + sig;
		}
		catch (Exception ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	static JsonNode decodeJwtPayload(String jws) {
		String[] parts = jws.split("\\.");
		if (parts.length < 2) {
			throw new InvalidApplePurchaseException("Apple signed payload was malformed");
		}
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
			return JSON.readTree(decoded);
		}
		catch (InvalidApplePurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidApplePurchaseException("Apple signed payload could not be decoded", ex);
		}
	}

	static String extractTransactionIdFromSignedTransaction(String signedTransactionInfo) {
		JsonNode claims = decodeJwtPayload(signedTransactionInfo);
		String transactionId = text(claims, "transactionId");
		if (transactionId == null) {
			throw new InvalidApplePurchaseException("Apple signed transaction lacked transactionId");
		}
		return transactionId;
	}

	static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static Instant epochMillis(JsonNode node, String field) {
		Instant value = optionalEpochMillis(node, field);
		if (value == null) {
			throw new InvalidApplePurchaseException("Apple transaction lacked " + field);
		}
		return value;
	}

	private static Instant optionalEpochMillis(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isNumber()) {
			return Instant.ofEpochMilli(value.asLong());
		}
		String text = value.asString();
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			return Instant.ofEpochMilli(Long.parseLong(text.trim()));
		}
		catch (NumberFormatException ex) {
			throw new InvalidApplePurchaseException("Apple timestamp was invalid for " + field, ex);
		}
	}

	private static String requireTransactionId(String transactionId) {
		if (transactionId == null || transactionId.isBlank()) {
			throw new InvalidApplePurchaseException("Apple transaction id is required");
		}
		return transactionId.trim();
	}

	private static PrivateKey parsePrivateKey(String pem) {
		try {
			String normalized = pem
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "")
					.replaceAll("\\s", "");
			byte[] decoded = Base64.getDecoder().decode(normalized);
			return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(decoded));
		}
		catch (Exception ex) {
			throw new IllegalStateException("uap.billing.apple.private-key-pem could not be parsed", ex);
		}
	}

	private static String base64Url(byte[] input) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
	}

	private static String padBase64(String value) {
		int mod = value.length() % 4;
		if (mod == 0) {
			return value;
		}
		return value + "====".substring(mod);
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
