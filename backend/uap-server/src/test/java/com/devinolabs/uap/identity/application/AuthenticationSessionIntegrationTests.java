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
import java.util.List;

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
import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.LockoutPolicy;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionRevocationReason;
import com.devinolabs.uap.identity.domain.TokenDigester;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, AuthenticationSessionIntegrationTests.MutableClockConfig.class })
class AuthenticationSessionIntegrationTests {

	private static final String PASSWORD = "SecurePass123!";

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private AuthenticateAccountUseCase authenticateAccountUseCase;

	@Autowired
	private RotateRefreshSessionUseCase rotateRefreshSessionUseCase;

	@Autowired
	private LogoutUseCase logoutUseCase;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private RefreshSessionRepository refreshSessionRepository;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private TokenDigester tokenDigester;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private LockoutPolicy lockoutPolicy;

	@Autowired
	private Duration refreshTokenTtl;

	@Autowired
	private IdentityTransactionService identityTransactionService;

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
	void unknownEmailAndWrongPasswordAreIndistinguishable() {
		registerVerified("known.user@example.com");

		assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
				"missing.user@example.com", PASSWORD, ClientMetadata.empty()))
				.isInstanceOf(InvalidCredentialsException.class);

		assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
				"known.user@example.com", "WrongPass123!", ClientMetadata.empty()))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void rejectsPendingAndDisabledAccounts() {
		registerAccountUseCase.register("pending.user@example.com", PASSWORD);
		assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
				"pending.user@example.com", PASSWORD, ClientMetadata.empty()))
				.isInstanceOf(EmailNotVerifiedException.class);

		AccountId disabledId = AccountId.generate();
		accountRepository.save(Account.rehydrate(
				disabledId,
				EmailAddress.of("disabled.user@example.com"),
				PasswordCredential.fromHash(passwordHasher.hash(PASSWORD)),
				AccountStatus.DISABLED,
				0,
				null,
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				0L));

		assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
				"disabled.user@example.com", PASSWORD, ClientMetadata.empty()))
				.isInstanceOf(AccountDisabledException.class);
	}

	@Test
	void incrementsFailuresLocksAndRejectsUntilExpiryThenAllowsLogin() throws Exception {
		registerVerified("lockout.user@example.com");

		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
					"lockout.user@example.com", "WrongPass123!", ClientMetadata.empty()))
					.isInstanceOf(InvalidCredentialsException.class);
		}

		Account locked = accountRepository.findByEmail(EmailAddress.of("lockout.user@example.com")).orElseThrow();
		assertThat(locked.status()).isEqualTo(AccountStatus.LOCKED);
		assertThat(locked.failedLoginAttempts()).isEqualTo(5);
		assertThat(locked.lockedUntil()).isEqualTo(Instant.parse("2026-07-24T12:15:00Z"));

		assertThatThrownBy(() -> authenticateAccountUseCase.authenticate(
				"lockout.user@example.com", PASSWORD, ClientMetadata.empty()))
				.isInstanceOf(AccountLockedException.class);

		mutableClock.setInstant(Instant.parse("2026-07-24T12:15:00Z"));
		AuthenticationResult result = authenticateAccountUseCase.authenticate(
				"lockout.user@example.com", PASSWORD, ClientMetadata.empty());

		Account unlocked = accountRepository.findById(result.accountId()).orElseThrow();
		assertThat(unlocked.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(unlocked.failedLoginAttempts()).isZero();
		assertThat(unlocked.lockedUntil()).isNull();
		assertAccessAndRefreshSafety(result, PASSWORD);
	}

	@Test
	void successfulLoginIssuesTokensAndPersistsRefreshDigestOnly() throws Exception {
		registerVerified("login.user@example.com");
		AuthenticationResult result = authenticateAccountUseCase.authenticate(
				"login.user@example.com", PASSWORD, ClientMetadata.empty());

		assertThat(accessTokenIssuer.verify(result.accessToken().token()).accountId())
				.isEqualTo(result.accountId());
		assertAccessAndRefreshSafety(result, PASSWORD);

		String digest = tokenDigester.digest(result.refreshToken());
		try (Connection connection = dataSource.getConnection();
				PreparedStatement byDigest = connection.prepareStatement(
						"SELECT COUNT(*) FROM refresh_sessions WHERE token_digest = ?");
				PreparedStatement byRaw = connection.prepareStatement(
						"SELECT COUNT(*) FROM refresh_sessions WHERE token_digest = ?")) {
			byDigest.setString(1, digest);
			byRaw.setString(1, result.refreshToken());
			try (ResultSet digestCount = byDigest.executeQuery();
					ResultSet rawCount = byRaw.executeQuery()) {
				assertThat(digestCount.next()).isTrue();
				assertThat(digestCount.getInt(1)).isEqualTo(1);
				assertThat(rawCount.next()).isTrue();
				assertThat(rawCount.getInt(1)).isZero();
			}
		}
	}

	@Test
	void rotatesRefreshSessionAndDetectsReuseAcrossAccountFamily() {
		registerVerified("rotate.user@example.com");
		AuthenticationResult first = authenticateAccountUseCase.authenticate(
				"rotate.user@example.com", PASSWORD, ClientMetadata.empty());

		AuthenticationResult rotated = rotateRefreshSessionUseCase.rotate(first.refreshToken());
		assertThat(rotated.refreshToken()).isNotEqualTo(first.refreshToken());
		assertThat(accessTokenIssuer.verify(rotated.accessToken().token()).accountId())
				.isEqualTo(first.accountId());

		RefreshSession oldSession = refreshSessionRepository
				.findByTokenDigest(tokenDigester.digest(first.refreshToken()))
				.orElseThrow();
		assertThat(oldSession.isRevoked()).isTrue();
		assertThat(oldSession.revocationReason()).isEqualTo(RefreshSessionRevocationReason.ROTATED);
		assertThat(oldSession.replacedBySessionId()).isEqualTo(rotated.refreshSessionId());

		assertThatThrownBy(() -> rotateRefreshSessionUseCase.rotate(first.refreshToken()))
				.isInstanceOf(RevokedRefreshTokenException.class);

		List<RefreshSession> active = refreshSessionRepository.findActiveByAccountId(
				first.accountId(), mutableClock.instant());
		assertThat(active).isEmpty();

		assertThatThrownBy(() -> rotateRefreshSessionUseCase.rotate(rotated.refreshToken()))
				.isInstanceOf(RevokedRefreshTokenException.class);
	}

	@Test
	void rejectsExpiredRefreshTokenAndSupportsLogoutFlows() {
		registerVerified("logout.user@example.com");
		AuthenticationResult first = authenticateAccountUseCase.authenticate(
				"logout.user@example.com", PASSWORD, ClientMetadata.empty());

		mutableClock.setInstant(Instant.parse("2026-08-24T12:00:00Z"));
		assertThatThrownBy(() -> rotateRefreshSessionUseCase.rotate(first.refreshToken()))
				.isInstanceOf(ExpiredRefreshTokenException.class);

		mutableClock.setInstant(Instant.parse("2026-07-24T12:00:00Z"));
		AuthenticationResult second = authenticateAccountUseCase.authenticate(
				"logout.user@example.com", PASSWORD, ClientMetadata.empty());
		AuthenticationResult third = authenticateAccountUseCase.authenticate(
				"logout.user@example.com", PASSWORD, ClientMetadata.empty());

		logoutUseCase.logout(second.refreshToken());
		assertThat(refreshSessionRepository.findByTokenDigest(tokenDigester.digest(second.refreshToken()))
				.orElseThrow().isRevoked()).isTrue();
		assertThatThrownBy(() -> rotateRefreshSessionUseCase.rotate(second.refreshToken()))
				.isInstanceOf(RevokedRefreshTokenException.class);

		logoutUseCase.logoutAll(third.accountId());
		assertThat(refreshSessionRepository.findActiveByAccountId(third.accountId(), mutableClock.instant()))
				.isEmpty();
		assertThatThrownBy(() -> rotateRefreshSessionUseCase.rotate(third.refreshToken()))
				.isInstanceOf(RevokedRefreshTokenException.class);
	}

	@Test
	void authenticationRollsBackWhenRefreshSessionPersistenceFails() {
		registerVerified("tx.user@example.com");
		AuthenticateAccountUseCase failingAuth = new AuthenticateAccountUseCase(
				accountRepository,
				new RefreshSessionRepository() {
					@Override
					public RefreshSession save(RefreshSession session) {
						throw new IllegalStateException("session save failed");
					}

					@Override
					public java.util.Optional<RefreshSession> findById(
							com.devinolabs.uap.identity.domain.RefreshSessionId id) {
						return refreshSessionRepository.findById(id);
					}

					@Override
					public java.util.Optional<RefreshSession> findByTokenDigest(String tokenDigest) {
						return refreshSessionRepository.findByTokenDigest(tokenDigest);
					}

					@Override
					public List<RefreshSession> findActiveByAccountId(AccountId accountId, Instant now) {
						return refreshSessionRepository.findActiveByAccountId(accountId, now);
					}
				},
				passwordHasher,
				accessTokenIssuer,
				tokenDigester,
				refreshTokenTtl,
				identityTransactionService,
				mutableClock);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> failingAuth.authenticate(
				"tx.user@example.com", PASSWORD, ClientMetadata.empty())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("session save failed");

		Account account = accountRepository.findByEmail(EmailAddress.of("tx.user@example.com")).orElseThrow();
		assertThat(account.failedLoginAttempts()).isZero();
	}

	private void registerVerified(String email) {
		registerAccountUseCase.register(email, PASSWORD);
		verifyEmailUseCase.verify(verificationNotifier.lastMessage().orElseThrow().rawToken());
	}

	private void assertAccessAndRefreshSafety(AuthenticationResult result, String rawPassword) {
		assertThat(result.toString()).doesNotContain(rawPassword);
		assertThat(result.toString()).doesNotContain(result.refreshToken());
		assertThat(result.accessToken().toString()).doesNotContain(result.accessToken().token());
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
