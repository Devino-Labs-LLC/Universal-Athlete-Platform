package com.devinolabs.uap.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.IssuedEmailVerificationToken;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.domain.PasswordPolicy;
import com.devinolabs.uap.identity.domain.PasswordPolicyViolation;
import com.devinolabs.uap.identity.domain.TokenDigester;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, RegistrationVerificationIntegrationTests.MutableClockConfig.class })
class RegistrationVerificationIntegrationTests {

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private TokenDigester tokenDigester;

	@Autowired
	private PasswordPolicy passwordPolicy;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private MutableClock mutableClock;

	@BeforeEach
	void setUp() {
		verificationNotifier.clear();
		mutableClock.setInstant(Instant.parse("2026-07-24T12:00:00Z"));
	}

	@Test
	void registersAccountHashingPasswordAndPersistingTokenDigestOnly() throws Exception {
		String rawPassword = "SecurePass123!";
		RegisterAccountResult result = registerAccountUseCase.register("  New.Athlete@Example.COM ", rawPassword);

		assertThat(result.email().value()).isEqualTo("new.athlete@example.com");
		assertThat(result.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
		assertThat(result.toString()).doesNotContain(rawPassword);
		assertThat(result.toString()).doesNotContain("$2a$");

		Account account = accountRepository.findById(result.accountId()).orElseThrow();
		assertThat(account.passwordCredential().hash()).isNotEqualTo(rawPassword);
		assertThat(passwordHasher.matches(rawPassword, account.passwordCredential())).isTrue();

		InMemoryVerificationNotifier.VerificationMessage message = verificationNotifier.lastMessage().orElseThrow();
		assertThat(message.email().value()).isEqualTo("new.athlete@example.com");
		String rawToken = message.rawToken();
		String expectedDigest = tokenDigester.digest(rawToken);

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT token_digest, account_id FROM email_verification_tokens WHERE token_digest = ?")) {
			statement.setString(1, expectedDigest);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				assertThat(resultSet.getString("token_digest")).isEqualTo(expectedDigest);
			}
		}

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT COUNT(*) FROM email_verification_tokens WHERE token_digest = ?")) {
			statement.setString(1, rawToken);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				assertThat(resultSet.getInt(1)).isZero();
			}
		}
	}

	@Test
	void rejectsDuplicateNormalizedEmailThroughUseCase() {
		registerAccountUseCase.register("dup.athlete@example.com", "SecurePass123!");

		assertThatThrownBy(() -> registerAccountUseCase.register("  Dup.Athlete@Example.COM ", "OtherPass123!"))
				.isInstanceOf(DuplicateAccountEmailException.class);
	}

	@Test
	void rejectsPasswordPolicyViolations() {
		assertThatThrownBy(() -> registerAccountUseCase.register("weak@example.com", "short1!"))
				.isInstanceOf(PasswordPolicyViolationException.class)
				.satisfies(ex -> assertThat(((PasswordPolicyViolationException) ex).violations())
						.contains(PasswordPolicyViolation.TOO_SHORT));
	}

	@Test
	void verifiesEmailActivatesAccountAndConsumesToken() {
		RegisterAccountResult registered = registerAccountUseCase.register("verify.me@example.com", "SecurePass123!");
		String rawToken = verificationNotifier.lastMessage().orElseThrow().rawToken();

		verifyEmailUseCase.verify(rawToken);

		Account account = accountRepository.findById(registered.accountId()).orElseThrow();
		assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.emailVerifiedAt()).isEqualTo(Instant.parse("2026-07-24T12:00:00Z"));

		EmailVerificationToken token = emailVerificationTokenRepository
				.findByTokenDigest(tokenDigester.digest(rawToken))
				.orElseThrow();
		assertThat(token.isConsumed()).isTrue();
		assertThat(token.consumedAt()).isEqualTo(Instant.parse("2026-07-24T12:00:00Z"));
	}

	@Test
	void rejectsInvalidExpiredAndAlreadyConsumedTokens() {
		assertThatThrownBy(() -> verifyEmailUseCase.verify("not-a-real-token"))
				.isInstanceOf(InvalidVerificationTokenException.class);
		assertThatThrownBy(() -> verifyEmailUseCase.verify(" "))
				.isInstanceOf(InvalidVerificationTokenException.class);

		RegisterAccountResult registered = registerAccountUseCase.register("expiry@example.com", "SecurePass123!");
		String rawToken = verificationNotifier.lastMessage().orElseThrow().rawToken();

		mutableClock.setInstant(Instant.parse("2026-07-25T12:00:00Z"));
		assertThatThrownBy(() -> verifyEmailUseCase.verify(rawToken))
				.isInstanceOf(ExpiredVerificationTokenException.class);
		assertThat(accountRepository.findById(registered.accountId()).orElseThrow().status())
				.isEqualTo(AccountStatus.PENDING_VERIFICATION);

		mutableClock.setInstant(Instant.parse("2026-07-24T12:00:00Z"));
		RegisterAccountResult second = registerAccountUseCase.register("once@example.com", "SecurePass123!");
		String secondToken = verificationNotifier.lastMessage().orElseThrow().rawToken();
		verifyEmailUseCase.verify(secondToken);
		assertThatThrownBy(() -> verifyEmailUseCase.verify(secondToken))
				.isInstanceOf(AlreadyConsumedVerificationTokenException.class);
		assertThat(accountRepository.findById(second.accountId()).orElseThrow().status())
				.isEqualTo(AccountStatus.ACTIVE);
	}

	@Test
	void registrationIsAtomicWhenNotificationFails() {
		VerificationNotificationPort failingNotifier = (email, rawToken) -> {
			throw new IllegalStateException("notification failed");
		};
		RegisterAccountUseCase failingRegistration = new RegisterAccountUseCase(
				accountRepository,
				emailVerificationTokenRepository,
				passwordPolicy,
				passwordHasher,
				tokenDigester,
				failingNotifier,
				mutableClock);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> failingRegistration
				.register("rollback@example.com", "SecurePass123!")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("notification failed");

		assertThat(accountRepository.existsByEmail(EmailAddress.of("rollback@example.com"))).isFalse();
	}

	@Test
	void verificationIsAtomicWhenTokenConsumeWouldFailAfterAccountUpdate() {
		Account account = accountRepository.save(Account.register(
				AccountId.generate(),
				EmailAddress.of("atomic.verify@example.com"),
				PasswordCredential.fromHash(passwordHasher.hash("SecurePass123!")),
				mutableClock));
		IssuedEmailVerificationToken issued = EmailVerificationToken.issue(
				account.id(),
				tokenDigester,
				mutableClock,
				Duration.ofHours(24),
				new java.security.SecureRandom());
		emailVerificationTokenRepository.save(issued.token());

		VerifyEmailUseCase useCase = new VerifyEmailUseCase(
				accountRepository,
				new EmailVerificationTokenRepository() {
					@Override
					public EmailVerificationToken save(EmailVerificationToken token) {
						throw new IllegalStateException("token save failed");
					}

					@Override
					public java.util.Optional<EmailVerificationToken> findById(
							com.devinolabs.uap.identity.domain.EmailVerificationTokenId id) {
						return emailVerificationTokenRepository.findById(id);
					}

					@Override
					public java.util.Optional<EmailVerificationToken> findByTokenDigest(String tokenDigest) {
						return emailVerificationTokenRepository.findByTokenDigest(tokenDigest);
					}
				},
				tokenDigester,
				mutableClock);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> useCase.verify(issued.rawToken())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("token save failed");

		Account reloaded = accountRepository.findById(account.id()).orElseThrow();
		assertThat(reloaded.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
		assertThat(reloaded.emailVerifiedAt()).isNull();
		assertThat(emailVerificationTokenRepository.findByTokenDigest(issued.token().tokenDigest()).orElseThrow()
				.isConsumed()).isFalse();
	}

	@TestConfiguration
	static class MutableClockConfig {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(Instant.parse("2026-07-24T12:00:00Z"));
		}

	}

	static final class MutableClock extends Clock {

		private Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void setInstant(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return Clock.fixed(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}

	}

}
