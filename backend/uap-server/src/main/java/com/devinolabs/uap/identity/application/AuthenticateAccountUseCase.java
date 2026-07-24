package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.domain.IssuedRefreshSession;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.TokenDigester;

@Service
public class AuthenticateAccountUseCase {

	/**
	 * Precomputed bcrypt hash used only to keep unknown-email timing closer to
	 * known-email password verification. Not a real account credential.
	 */
	static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final AccountRepository accountRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final PasswordHasher passwordHasher;
	private final AccessTokenIssuer accessTokenIssuer;
	private final TokenDigester tokenDigester;
	private final Duration refreshTokenTtl;
	private final IdentityTransactionService identityTransactionService;
	private final Clock clock;

	public AuthenticateAccountUseCase(
			AccountRepository accountRepository,
			RefreshSessionRepository refreshSessionRepository,
			PasswordHasher passwordHasher,
			AccessTokenIssuer accessTokenIssuer,
			TokenDigester tokenDigester,
			Duration refreshTokenTtl,
			IdentityTransactionService identityTransactionService,
			Clock clock) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.passwordHasher = Objects.requireNonNull(passwordHasher);
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
		this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl);
		this.identityTransactionService = Objects.requireNonNull(identityTransactionService);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AuthenticationResult authenticate(String email, CharSequence rawPassword, ClientMetadata clientMetadata) {
		Objects.requireNonNull(clientMetadata, "clientMetadata must not be null");
		EmailAddress emailAddress = EmailAddress.of(email);
		Optional<Account> accountOptional = accountRepository.findByEmail(emailAddress);

		if (accountOptional.isEmpty()) {
			passwordHasher.matches(rawPassword == null ? "" : rawPassword, PasswordCredential.fromHash(DUMMY_PASSWORD_HASH));
			throw new InvalidCredentialsException();
		}

		Account account = accountOptional.get();
		if (account.status() == AccountStatus.PENDING_VERIFICATION) {
			throw new EmailNotVerifiedException();
		}
		if (account.status() == AccountStatus.DISABLED) {
			throw new AccountDisabledException();
		}

		account.clearExpiredLock(clock);
		if (account.isCurrentlyLocked(clock)) {
			accountRepository.save(account);
			throw new AccountLockedException(account.lockedUntil());
		}

		if (rawPassword == null || !passwordHasher.matches(rawPassword, account.passwordCredential())) {
			identityTransactionService.recordFailedAuthentication(account.id());
			throw new InvalidCredentialsException();
		}

		account.recordSuccessfulAuthentication(clock);
		account = accountRepository.save(account);

		IssuedAccessToken accessToken = accessTokenIssuer.issue(account.id());
		IssuedRefreshSession issuedRefreshSession = RefreshSession.issue(
				account.id(),
				tokenDigester,
				refreshTokenTtl,
				clock);
		refreshSessionRepository.save(issuedRefreshSession.session());

		return new AuthenticationResult(
				account.id(),
				accessToken,
				issuedRefreshSession.rawToken(),
				issuedRefreshSession.session().id());
	}

}
