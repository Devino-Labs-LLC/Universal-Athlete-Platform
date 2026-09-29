package com.devinolabs.uap.billing.infrastructure.google;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Play Developer API client (subscriptions v2). Never logs the private key or bearer token.
 */
@Component
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
class HttpGooglePlayDeveloperApiClient implements GooglePlayDeveloperApiClient {

	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
	private static final String API_BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GooglePlayBillingProperties properties;
	private final RestTemplate restTemplate;
	private final PrivateKey privateKey;
	private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

	HttpGooglePlayDeveloperApiClient(GooglePlayBillingProperties properties) {
		this(properties, new RestTemplate(), parsePrivateKey(properties.privateKeyPem()));
	}

	HttpGooglePlayDeveloperApiClient(
			GooglePlayBillingProperties properties,
			RestTemplate restTemplate,
			PrivateKey privateKey) {
		this.properties = Objects.requireNonNull(properties);
		this.restTemplate = Objects.requireNonNull(restTemplate);
		this.privateKey = Objects.requireNonNull(privateKey);
	}

	@Override
	public SubscriptionPurchase getSubscriptionPurchase(String purchaseToken) {
		String normalized = requirePurchaseToken(purchaseToken);
		String encodedToken = UriUtils.encodePathSegment(normalized, StandardCharsets.UTF_8);
		String url = API_BASE + "/applications/"
				+ UriUtils.encodePathSegment(properties.getPackageName(), StandardCharsets.UTF_8)
				+ "/purchases/subscriptionsv2/tokens/" + encodedToken;
		try {
			RequestEntity<Void> request = RequestEntity.get(url)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
					.accept(MediaType.APPLICATION_JSON)
					.build();
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new InvalidGooglePlayPurchaseException("Google Play purchase was not found");
			}
			return parseSubscriptionResponse(normalized, response.getBody());
		}
		catch (InvalidGooglePlayPurchaseException ex) {
			throw ex;
		}
		catch (HttpStatusCodeException ex) {
			if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 400) {
				throw new InvalidGooglePlayPurchaseException("Google Play purchase was not found");
			}
			throw new BillingProviderUnavailableException(ex);
		}
		catch (RestClientException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	SubscriptionPurchase parseSubscriptionResponse(String purchaseToken, String body) {
		try {
			JsonNode root = JSON.readTree(body);
			String subscriptionState = text(root, "subscriptionState");
			JsonNode lineItems = root.path("lineItems");
			if (!lineItems.isArray() || lineItems.isEmpty()) {
				throw new InvalidGooglePlayPurchaseException("Google Play purchase payload was incomplete");
			}
			JsonNode primary = lineItems.get(0);
			String productId = text(primary, "productId");
			if (productId == null) {
				throw new InvalidGooglePlayPurchaseException("Google Play purchase lacked productId");
			}
			Instant startTime = parseInstant(root, "startTime");
			if (startTime == null) {
				startTime = Instant.now();
			}
			Instant expiryTime = parseInstant(primary, "expiryTime");
			Instant providerStateAsOf = Instant.now();
			boolean autoRenewEnabled = true;
			JsonNode autoRenewing = primary.path("autoRenewingPlan");
			if (autoRenewing.has("autoRenewEnabled")) {
				autoRenewEnabled = autoRenewing.path("autoRenewEnabled").asBoolean(true);
			}
			JsonNode externalIds = root.path("externalAccountIdentifiers");
			String obfuscatedAccountId = text(externalIds, "obfuscatedExternalAccountId");
			boolean testPurchase = root.has("testPurchase") && !root.path("testPurchase").isNull()
					&& !root.path("testPurchase").isMissingNode();
			String orderId = text(root, "latestOrderId");
			return new SubscriptionPurchase(
					purchaseToken,
					orderId,
					productId,
					properties.getPackageName(),
					testPurchase,
					obfuscatedAccountId,
					startTime,
					expiryTime,
					providerStateAsOf,
					autoRenewEnabled,
					subscriptionState);
		}
		catch (InvalidGooglePlayPurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidGooglePlayPurchaseException("Google Play purchase payload was invalid", ex);
		}
	}

	private String accessToken() {
		CachedToken current = cachedToken.get();
		Instant now = Instant.now();
		if (current != null && current.expiresAt().isAfter(now.plusSeconds(60))) {
			return current.token();
		}
		String token = fetchAccessToken();
		cachedToken.set(new CachedToken(token, now.plusSeconds(3000)));
		return token;
	}

	private String fetchAccessToken() {
		try {
			String assertion = createServiceAccountJwt();
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
			form.add("assertion", assertion);
			RequestEntity<MultiValueMap<String, String>> request = RequestEntity.post(TOKEN_URL)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.body(form);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new BillingProviderUnavailableException(
						new IllegalStateException("Google OAuth token exchange failed"));
			}
			JsonNode root = JSON.readTree(response.getBody());
			String accessToken = text(root, "access_token");
			if (accessToken == null) {
				throw new BillingProviderUnavailableException(
						new IllegalStateException("Google OAuth token response lacked access_token"));
			}
			return accessToken;
		}
		catch (BillingProviderUnavailableException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	private String createServiceAccountJwt() {
		try {
			long now = Instant.now().getEpochSecond();
			String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
			String payloadJson = "{"
					+ "\"iss\":\"" + escape(properties.clientEmail()) + "\","
					+ "\"scope\":\"" + SCOPE + "\","
					+ "\"aud\":\"" + TOKEN_URL + "\","
					+ "\"iat\":" + now + ","
					+ "\"exp\":" + (now + 3600)
					+ "}";
			String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
			String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
			String signingInput = header + "." + payload;
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(privateKey);
			signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
			return signingInput + "." + base64Url(signature.sign());
		}
		catch (Exception ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static Instant parseInstant(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(text.trim());
		}
		catch (RuntimeException ex) {
			throw new InvalidGooglePlayPurchaseException("Google Play timestamp was invalid for " + field, ex);
		}
	}

	private static String requirePurchaseToken(String purchaseToken) {
		if (purchaseToken == null || purchaseToken.isBlank()) {
			throw new InvalidGooglePlayPurchaseException("Google Play purchase token is required");
		}
		return purchaseToken.trim();
	}

	private static PrivateKey parsePrivateKey(String pem) {
		try {
			String normalized = pem
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "")
					.replaceAll("\\s", "");
			byte[] decoded = Base64.getDecoder().decode(normalized);
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"uap.billing.google-play.service-account-json private_key could not be parsed", ex);
		}
	}

	private static String base64Url(byte[] input) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private record CachedToken(String token, Instant expiresAt) {
	}

}
