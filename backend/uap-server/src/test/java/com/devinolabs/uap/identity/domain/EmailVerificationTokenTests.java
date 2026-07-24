package com.devinolabs.uap.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class EmailVerificationTokenTests {

	private static final Clock ISSUE_CLOCK = Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC);
	private final TokenDigester tokenDigester = rawToken -> HexFormat.of()
			.formatHex(("digest:" + rawToken).getBytes());

	@Test
	void issueCreatesDigestOnlyTokenWithExpiry() {
		IssuedEmailVerificationToken issued = EmailVerificationToken.issue(
				AccountId.generate(),
				tokenDigester,
				ISSUE_CLOCK,
				Duration.ofHours(24),
				new SecureRandom());

		assertThat(issued.rawToken()).isNotBlank();
		assertThat(issued.token().tokenDigest()).isEqualTo(tokenDigester.digest(issued.rawToken()));
		assertThat(issued.token().tokenDigest()).doesNotContain(issued.rawToken());
		assertThat(issued.token().expiresAt()).isEqualTo(Instant.parse("2026-07-25T12:00:00Z"));
		assertThat(issued.token().isConsumed()).isFalse();
		assertThat(issued.toString()).doesNotContain(issued.rawToken());
		assertThat(issued.token().toString()).doesNotContain(issued.token().tokenDigest());
	}

	@Test
	void rejectsExpiredAndAlreadyConsumedTokens() {
		IssuedEmailVerificationToken issued = EmailVerificationToken.issue(
				AccountId.generate(),
				tokenDigester,
				ISSUE_CLOCK,
				Duration.ofMinutes(30),
				new SecureRandom());
		EmailVerificationToken token = issued.token();

		Clock expiredClock = Clock.fixed(Instant.parse("2026-07-24T12:31:00Z"), ZoneOffset.UTC);
		assertThat(token.isExpired(expiredClock)).isTrue();
		assertThatThrownBy(() -> token.ensureUsable(expiredClock)).isInstanceOf(TokenExpiredException.class);

		EmailVerificationToken usable = EmailVerificationToken.issue(
				AccountId.generate(),
				tokenDigester,
				ISSUE_CLOCK,
				Duration.ofHours(1),
				new SecureRandom()).token();
		usable.consume(ISSUE_CLOCK);
		assertThat(usable.isConsumed()).isTrue();
		assertThatThrownBy(() -> usable.ensureUsable(ISSUE_CLOCK)).isInstanceOf(TokenAlreadyConsumedException.class);
		assertThatThrownBy(() -> usable.consume(ISSUE_CLOCK)).isInstanceOf(TokenAlreadyConsumedException.class);
	}

}
