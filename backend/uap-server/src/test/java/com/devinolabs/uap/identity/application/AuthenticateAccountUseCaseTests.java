package com.devinolabs.uap.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.identity.domain.AccessTokenClaims;
import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.domain.LockoutPolicy;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionId;
import com.devinolabs.uap.identity.domain.TokenDigester;

class AuthenticateAccountUseCaseTests {

	@Test
	void unknownAccountPerformsPasswordVerificationAndDoesNotAuthenticate() {
		AtomicInteger hashCalls = new AtomicInteger();
		AtomicInteger matchCalls = new AtomicInteger();
		RecordingPasswordHasher passwordHasher = new RecordingPasswordHasher(hashCalls, matchCalls);
		AuthenticateAccountUseCase useCase = new AuthenticateAccountUseCase(
				new EmptyAccountRepository(),
				new UnusedRefreshSessionRepository(),
				passwordHasher,
				new UnusedAccessTokenIssuer(),
				unusedTokenDigester(),
				Duration.ofHours(1),
				unusedIdentityTransactionService(),
				Clock.systemUTC());

		assertThat(hashCalls.get()).isEqualTo(1);
		assertThatThrownBy(() -> useCase.authenticate(
				"missing.user@example.com",
				"AnyPass123!",
				ClientMetadata.empty()))
				.isInstanceOf(InvalidCredentialsException.class);
		assertThat(matchCalls.get()).isEqualTo(1);
		assertThat(passwordHasher.lastVerifiedCredentialHash()).isEqualTo("work-factor-anchor");
	}

	private static TokenDigester unusedTokenDigester() {
		return rawToken -> {
			throw new UnsupportedOperationException("token digest should not run for unknown accounts");
		};
	}

	private static IdentityTransactionService unusedIdentityTransactionService() {
		return new IdentityTransactionService(
				new EmptyAccountRepository(),
				new UnusedRefreshSessionRepository(),
				new LockoutPolicy() {
					@Override
					public int maxFailedAttempts() {
						return 5;
					}

					@Override
					public Duration lockDuration() {
						return Duration.ofMinutes(15);
					}
				},
				Clock.systemUTC());
	}

	private static final class RecordingPasswordHasher implements PasswordHasher {

		private final AtomicInteger hashCalls;
		private final AtomicInteger matchCalls;
		private String lastVerifiedCredentialHash;

		private RecordingPasswordHasher(AtomicInteger hashCalls, AtomicInteger matchCalls) {
			this.hashCalls = hashCalls;
			this.matchCalls = matchCalls;
		}

		@Override
		public String hash(CharSequence rawPassword) {
			hashCalls.incrementAndGet();
			return "work-factor-anchor";
		}

		@Override
		public boolean matches(CharSequence rawPassword, PasswordCredential credential) {
			matchCalls.incrementAndGet();
			lastVerifiedCredentialHash = credential.hash();
			return false;
		}

		private String lastVerifiedCredentialHash() {
			return lastVerifiedCredentialHash;
		}

	}

	private static final class EmptyAccountRepository implements AccountRepository {

		@Override
		public Account save(Account account) {
			throw new UnsupportedOperationException("unknown accounts must not be persisted");
		}

		@Override
		public Optional<Account> findById(AccountId id) {
			return Optional.empty();
		}

		@Override
		public Optional<Account> findByEmail(EmailAddress email) {
			return Optional.empty();
		}

		@Override
		public boolean existsByEmail(EmailAddress email) {
			return false;
		}

	}

	private static final class UnusedRefreshSessionRepository implements RefreshSessionRepository {

		@Override
		public RefreshSession save(RefreshSession session) {
			throw new UnsupportedOperationException("refresh sessions must not be created for unknown accounts");
		}

		@Override
		public Optional<RefreshSession> findById(RefreshSessionId id) {
			return Optional.empty();
		}

		@Override
		public Optional<RefreshSession> findByTokenDigest(String tokenDigest) {
			return Optional.empty();
		}

		@Override
		public List<RefreshSession> findActiveByAccountId(AccountId accountId, Instant now) {
			return List.of();
		}

	}

	private static final class UnusedAccessTokenIssuer implements AccessTokenIssuer {

		@Override
		public IssuedAccessToken issue(AccountId accountId) {
			throw new UnsupportedOperationException("access tokens must not be issued for unknown accounts");
		}

		@Override
		public AccessTokenClaims verify(String accessToken) {
			throw new UnsupportedOperationException("token verification is unused in this test");
		}

	}

}
