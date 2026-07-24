package com.devinolabs.uap.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.identity.domain.AccessTokenClaims;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.infrastructure.IdentityAuthProperties;

class HmacJwtAccessTokenIssuerTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC);
	private final IdentityAuthProperties properties = properties();
	private final HmacJwtAccessTokenIssuer issuer = new HmacJwtAccessTokenIssuer(properties, clock);

	@Test
	void issuesAndVerifiesMinimalClaims() {
		AccountId accountId = AccountId.generate();

		IssuedAccessToken issued = issuer.issue(accountId);
		AccessTokenClaims claims = issuer.verify(issued.token());

		assertThat(issued.token().split("\\.", -1)).hasSize(3);
		assertThat(issued.tokenId()).isNotBlank();
		assertThat(claims.accountId()).isEqualTo(accountId);
		assertThat(claims.issuedAt()).isEqualTo(Instant.parse("2026-07-24T12:00:00Z"));
		assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-07-24T12:15:00Z"));
		assertThat(issued.toString()).doesNotContain(issued.token());
	}

	@Test
	void rejectsTamperedExpiredMalformedAndNonHs256Tokens() throws Exception {
		IssuedAccessToken issued = issuer.issue(AccountId.generate());
		assertThatThrownBy(() -> issuer.verify(issued.token() + "x"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> issuer.verify("only.two"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> issuer.verify("a.b."))
				.isInstanceOf(IllegalArgumentException.class);

		String[] parts = issued.token().split("\\.", -1);
		String rs256Header = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		String signingInput = rs256Header + "." + parts[1];
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(
				properties.getAccessTokenSecret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256"));
		String signature = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
		String rs256Token = signingInput + "." + signature;
		assertThatThrownBy(() -> issuer.verify(rs256Token))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("algorithm");

		HmacJwtAccessTokenIssuer laterIssuer = new HmacJwtAccessTokenIssuer(
				properties,
				Clock.fixed(Instant.parse("2026-07-24T12:15:00Z"), ZoneOffset.UTC));
		assertThatThrownBy(() -> laterIssuer.verify(issued.token()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("expired");
	}

	@Test
	void rejectsWeakOrInvalidAuthSettings() {
		IdentityAuthProperties weak = new IdentityAuthProperties();
		weak.setAccessTokenSecret("too-short");
		assertThatThrownBy(weak::validate).isInstanceOf(IllegalStateException.class);

		IdentityAuthProperties invalidTtl = properties();
		invalidTtl.setAccessTokenTtl(Duration.ZERO);
		assertThatThrownBy(invalidTtl::validate).isInstanceOf(IllegalStateException.class);

		IdentityAuthProperties invalidAttempts = properties();
		invalidAttempts.setMaxFailedAttempts(0);
		assertThatThrownBy(invalidAttempts::validate).isInstanceOf(IllegalStateException.class);
	}

	private static IdentityAuthProperties properties() {
		IdentityAuthProperties properties = new IdentityAuthProperties();
		properties.setAccessTokenSecret("test-access-token-secret-value-32chars-min");
		properties.setAccessTokenTtl(Duration.ofMinutes(15));
		properties.setRefreshTokenTtl(Duration.ofDays(30));
		properties.setMaxFailedAttempts(5);
		properties.setLockoutDuration(Duration.ofMinutes(15));
		return properties;
	}

}
