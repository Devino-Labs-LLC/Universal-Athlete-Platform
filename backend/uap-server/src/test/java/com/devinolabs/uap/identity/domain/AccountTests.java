package com.devinolabs.uap.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AccountTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerCreatesPendingAccountWithResetCounters() {
		AccountId id = AccountId.generate();
		EmailAddress email = EmailAddress.of("athlete@example.com");
		PasswordCredential credential = PasswordCredential.fromHash("$2a$10$hashed-password-value");

		Account account = Account.register(id, email, credential, FIXED_CLOCK);

		assertThat(account.id()).isEqualTo(id);
		assertThat(account.email()).isEqualTo(email);
		assertThat(account.passwordCredential()).isEqualTo(credential);
		assertThat(account.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
		assertThat(account.failedLoginAttempts()).isZero();
		assertThat(account.lockedUntil()).isNull();
		assertThat(account.emailVerifiedAt()).isNull();
		assertThat(account.createdAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));
		assertThat(account.updatedAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));
		assertThat(account.version()).isZero();
	}

	@Test
	void registerRejectsMissingRequiredValues() {
		AccountId id = AccountId.generate();
		EmailAddress email = EmailAddress.of("athlete@example.com");
		PasswordCredential credential = PasswordCredential.fromHash("$2a$10$hashed-password-value");

		assertThatThrownBy(() -> Account.register(null, email, credential))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Account.register(id, null, credential))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Account.register(id, email, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void passwordCredentialRejectsBlankHashAndHidesValueInToString() {
		assertThatThrownBy(() -> PasswordCredential.fromHash(" "))
				.isInstanceOf(IllegalArgumentException.class);

		PasswordCredential credential = PasswordCredential.fromHash("$2a$10$hashed-password-value");
		assertThat(credential.toString()).doesNotContain("$2a$10$hashed-password-value");
		assertThat(credential.toString()).contains("****");
	}

}
