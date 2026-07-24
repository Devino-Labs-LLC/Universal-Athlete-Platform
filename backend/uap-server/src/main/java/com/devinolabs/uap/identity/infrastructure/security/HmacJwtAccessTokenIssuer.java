package com.devinolabs.uap.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.devinolabs.uap.identity.domain.AccessTokenClaims;
import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.infrastructure.IdentityAuthProperties;

public final class HmacJwtAccessTokenIssuer implements AccessTokenIssuer {

	private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
	private static final Pattern CLAIM_STRING = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern CLAIM_NUMBER = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");

	private final IdentityAuthProperties properties;
	private final Clock clock;

	public HmacJwtAccessTokenIssuer(IdentityAuthProperties properties, Clock clock) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public IssuedAccessToken issue(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
		String tokenId = UUID.randomUUID().toString();

		String payloadJson = "{"
				+ "\"jti\":\"" + tokenId + "\","
				+ "\"sub\":\"" + accountId.value() + "\","
				+ "\"iat\":" + issuedAt.getEpochSecond() + ","
				+ "\"exp\":" + expiresAt.getEpochSecond()
				+ "}";

		String signingInput = encode(HEADER_JSON) + "." + encode(payloadJson);
		String token = signingInput + "." + sign(signingInput);
		return new IssuedAccessToken(token, new AccessTokenClaims(tokenId, accountId, issuedAt, expiresAt));
	}

	@Override
	public AccessTokenClaims verify(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("Access token must not be blank");
		}

		String[] parts = accessToken.split("\\.", -1);
		if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
			throw new IllegalArgumentException("Access token format is invalid");
		}

		String signingInput = parts[0] + "." + parts[1];
		byte[] expectedSignature;
		byte[] providedSignature;
		try {
			expectedSignature = BASE64_URL_DECODER.decode(sign(signingInput));
			providedSignature = BASE64_URL_DECODER.decode(parts[2]);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Access token signature is invalid", ex);
		}
		if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
			throw new IllegalArgumentException("Access token signature is invalid");
		}

		String headerJson = decode(parts[0]);
		requireHs256Algorithm(headerJson);

		String payloadJson = decode(parts[1]);
		String tokenId = requiredStringClaim(payloadJson, "jti");
		AccountId accountId = AccountId.of(requiredStringClaim(payloadJson, "sub"));
		Instant issuedAt = Instant.ofEpochSecond(requiredLongClaim(payloadJson, "iat"));
		Instant expiresAt = Instant.ofEpochSecond(requiredLongClaim(payloadJson, "exp"));
		Instant now = Instant.now(clock);
		if (expiresAt.isBefore(issuedAt) || expiresAt.equals(issuedAt)) {
			throw new IllegalArgumentException("Access token claims are invalid");
		}
		if (issuedAt.isAfter(now)) {
			throw new IllegalArgumentException("Access token claims are invalid");
		}
		if (!now.isBefore(expiresAt)) {
			throw new IllegalArgumentException("Access token has expired");
		}
		return new AccessTokenClaims(tokenId, accountId, issuedAt, expiresAt);
	}

	private static void requireHs256Algorithm(String headerJson) {
		String algorithm = requiredStringClaim(headerJson, "alg");
		if (!"HS256".equals(algorithm)) {
			throw new IllegalArgumentException("Access token algorithm is invalid");
		}
	}

	private String sign(String signingInput) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.getAccessTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return BASE64_URL.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to sign access token", ex);
		}
	}

	private static String encode(String value) {
		return BASE64_URL.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String decode(String value) {
		try {
			return new String(BASE64_URL_DECODER.decode(value), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Access token encoding is invalid", ex);
		}
	}

	private static String requiredStringClaim(String json, String claim) {
		Matcher matcher = Pattern.compile(String.format(CLAIM_STRING.pattern(), Pattern.quote(claim))).matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Access token claims are invalid");
		}
		return matcher.group(1);
	}

	private static long requiredLongClaim(String json, String claim) {
		Matcher matcher = Pattern.compile(String.format(CLAIM_NUMBER.pattern(), Pattern.quote(claim))).matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Access token claims are invalid");
		}
		return Long.parseLong(matcher.group(1));
	}

}
