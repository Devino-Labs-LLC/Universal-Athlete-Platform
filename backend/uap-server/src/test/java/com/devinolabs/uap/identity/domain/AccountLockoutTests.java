package com.devinolabs.uap.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AccountLockoutTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC);
	private final LockoutPolicy lockoutPolicy = new LockoutPolicy() {
		@Override
		public int maxFailedAttempts() {
			return 5;
		}

		@Override
		public Duration lockDuration() {
			return Duration.ofMinutes(15);
		}
	};

	@Test
	void recordsFailedAttemptsAndLocksAfterThreshold() {
		Account account = activeAccount();

		for (int attempt = 1; attempt <= 4; attempt++) {
			account.recordFailedAuthentication(lockoutPolicy, CLOCK);
			assertThat(account.failedLoginAttempts()).isEqualTo(attempt);
			assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
		}

		account.recordFailedAuthentication(lockoutPolicy, CLOCK);

		assertThat(account.failedLoginAttempts()).isEqualTo(5);
		assertThat(account.status()).isEqualTo(AccountStatus.LOCKED);
		assertThat(account.lockedUntil()).isEqualTo(Instant.parse("2026-07-24T12:15:00Z"));
		assertThat(account.isCurrentlyLocked(CLOCK)).isTrue();
	}

	@Test
	void clearsExpiredLockAndSuccessfulLoginResetsAttempts() {
		Account account = activeAccount();
		for (int i = 0; i < 5; i++) {
			account.recordFailedAuthentication(lockoutPolicy, CLOCK);
		}

		Clock afterExpiry = Clock.fixed(Instant.parse("2026-07-24T12:15:00Z"), ZoneOffset.UTC);
		assertThat(account.isCurrentlyLocked(afterExpiry)).isFalse();
		account.clearExpiredLock(afterExpiry);
		assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.failedLoginAttempts()).isZero();
		assertThat(account.lockedUntil()).isNull();

		account.recordFailedAuthentication(lockoutPolicy, afterExpiry);
		account.recordSuccessfulAuthentication(afterExpiry);
		assertThat(account.failedLoginAttempts()).isZero();
		assertThat(account.lockedUntil()).isNull();
	}

	@Test
	void rejectsInvalidLockStateCombinations() {
		assertThatThrownBy(() -> Account.rehydrate(
				AccountId.generate(),
				EmailAddress.of("athlete@example.com"),
				PasswordCredential.fromHash("$2a$10$hashed"),
				AccountStatus.LOCKED,
				5,
				null,
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				0L)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Account.rehydrate(
				AccountId.generate(),
				EmailAddress.of("athlete@example.com"),
				PasswordCredential.fromHash("$2a$10$hashed"),
				AccountStatus.ACTIVE,
				0,
				Instant.parse("2026-07-24T12:15:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				0L)).isInstanceOf(IllegalArgumentException.class);
	}

	private static Account activeAccount() {
		return Account.rehydrate(
				AccountId.generate(),
				EmailAddress.of("athlete@example.com"),
				PasswordCredential.fromHash("$2a$10$hashed"),
				AccountStatus.ACTIVE,
				0,
				null,
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				0L);
	}

}
