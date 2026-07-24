package com.devinolabs.uap.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AccountVerificationTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void verifyEmailActivatesAccountAndSetsVerifiedAt() {
		Account account = Account.register(
				AccountId.generate(),
				EmailAddress.of("athlete@example.com"),
				PasswordCredential.fromHash("$2a$10$hashed-password-value"),
				Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC));

		account.verifyEmail(FIXED_CLOCK);

		assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.emailVerifiedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
		assertThat(account.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
	}

}
