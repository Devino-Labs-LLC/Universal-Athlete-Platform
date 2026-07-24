package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.TokenAlreadyConsumedException;
import com.devinolabs.uap.identity.domain.TokenDigester;
import com.devinolabs.uap.identity.domain.TokenExpiredException;

@Service
public class VerifyEmailUseCase {

	private final AccountRepository accountRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final TokenDigester tokenDigester;
	private final Clock clock;

	public VerifyEmailUseCase(
			AccountRepository accountRepository,
			EmailVerificationTokenRepository emailVerificationTokenRepository,
			TokenDigester tokenDigester,
			Clock clock) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
		this.emailVerificationTokenRepository = Objects.requireNonNull(emailVerificationTokenRepository);
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void verify(String rawVerificationToken) {
		if (rawVerificationToken == null || rawVerificationToken.isBlank()) {
			throw new InvalidVerificationTokenException();
		}

		String tokenDigest = tokenDigester.digest(rawVerificationToken);
		EmailVerificationToken token = emailVerificationTokenRepository.findByTokenDigest(tokenDigest)
				.orElseThrow(InvalidVerificationTokenException::new);

		try {
			token.ensureUsable(clock);
		}
		catch (TokenExpiredException ex) {
			throw new ExpiredVerificationTokenException(ex);
		}
		catch (TokenAlreadyConsumedException ex) {
			throw new AlreadyConsumedVerificationTokenException(ex);
		}

		Account account = accountRepository.findById(token.accountId())
				.orElseThrow(InvalidVerificationTokenException::new);

		account.verifyEmail(clock);
		token.consume(clock);

		accountRepository.save(account);
		emailVerificationTokenRepository.save(token);
	}

}
