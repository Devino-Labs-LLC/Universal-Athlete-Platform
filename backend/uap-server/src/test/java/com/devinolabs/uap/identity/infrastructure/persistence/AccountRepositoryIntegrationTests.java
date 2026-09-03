package com.devinolabs.uap.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.identity.application.AccountRepository;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountRepositoryIntegrationTests {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Test
	void persistsAndReloadsAccountRoundTrip() {
		AccountId id = AccountId.generate();
		EmailAddress email = EmailAddress.of("  Round.Trip@Example.COM ");
		PasswordCredential credential = PasswordCredential.fromHash(passwordHasher.hash("RoundTrip-Pass1!"));
		Account account = Account.register(id, email, credential);

		Account saved = accountRepository.save(account);
		Account reloaded = accountRepository.findById(id).orElseThrow();

		assertThat(saved.email().value()).isEqualTo("round.trip@example.com");
		assertThat(reloaded.id()).isEqualTo(id);
		assertThat(reloaded.email().value()).isEqualTo("round.trip@example.com");
		assertThat(reloaded.passwordCredential().hash()).isEqualTo(credential.hash());
		assertThat(reloaded.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
		assertThat(reloaded.failedLoginAttempts()).isZero();
		assertThat(reloaded.lockedUntil()).isNull();
		assertThat(reloaded.emailVerifiedAt()).isNull();
		// DATETIME(6) stores microseconds and may round leftover nanos.
		assertThat(reloaded.createdAt()).isCloseTo(account.createdAt(), within(1, ChronoUnit.MICROS));
		assertThat(reloaded.updatedAt()).isCloseTo(account.updatedAt(), within(1, ChronoUnit.MICROS));
		assertThat(accountRepository.findByEmail(EmailAddress.of("round.trip@example.com"))).isPresent();
		assertThat(accountRepository.existsByEmail(EmailAddress.of("round.trip@example.com"))).isTrue();
	}

	@Test
	void rejectsDuplicateNormalizedEmails() {
		EmailAddress email = EmailAddress.of("unique.athlete@example.com");
		accountRepository.save(Account.register(
				AccountId.generate(),
				email,
				PasswordCredential.fromHash(passwordHasher.hash("First-Pass1!"))));

		assertThatThrownBy(() -> accountRepository.save(Account.register(
				AccountId.generate(),
				EmailAddress.of("  Unique.Athlete@Example.COM "),
				PasswordCredential.fromHash(passwordHasher.hash("Second-Pass1!")))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
