package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.IssuedEmailVerificationToken;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.domain.PasswordPolicy;
import com.devinolabs.uap.identity.domain.PasswordPolicyResult;
import com.devinolabs.uap.identity.domain.TokenDigester;

@Service
public class RegisterAccountUseCase {

	private final AccountRepository accountRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordPolicy passwordPolicy;
	private final PasswordHasher passwordHasher;
	private final TokenDigester tokenDigester;
	private final VerificationNotificationPort verificationNotificationPort;
	private final Clock clock;

	public RegisterAccountUseCase(
			AccountRepository accountRepository,
			EmailVerificationTokenRepository emailVerificationTokenRepository,
			PasswordPolicy passwordPolicy,
			PasswordHasher passwordHasher,
			TokenDigester tokenDigester,
			VerificationNotificationPort verificationNotificationPort,
			Clock clock) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
		this.emailVerificationTokenRepository = Objects.requireNonNull(emailVerificationTokenRepository);
		this.passwordPolicy = Objects.requireNonNull(passwordPolicy);
		this.passwordHasher = Objects.requireNonNull(passwordHasher);
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
		this.verificationNotificationPort = Objects.requireNonNull(verificationNotificationPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public RegisterAccountResult register(String email, CharSequence rawPassword) {
		EmailAddress emailAddress = EmailAddress.of(email);
		if (accountRepository.existsByEmail(emailAddress)) {
			throw new DuplicateAccountEmailException(emailAddress);
		}

		PasswordPolicyResult policyResult = passwordPolicy.validate(rawPassword);
		if (!policyResult.isValid()) {
			throw new PasswordPolicyViolationException(policyResult.violations());
		}

		PasswordCredential credential = PasswordCredential.fromHash(passwordHasher.hash(rawPassword));
		Account account = Account.register(AccountId.generate(), emailAddress, credential, clock);

		try {
			account = accountRepository.save(account);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateAccountEmailException(emailAddress, ex);
		}

		IssuedEmailVerificationToken issued = EmailVerificationToken.issue(account.id(), tokenDigester, clock);
		emailVerificationTokenRepository.save(issued.token());
		verificationNotificationPort.sendVerificationMessage(account.email(), issued.rawToken());

		return new RegisterAccountResult(account.id(), account.email(), account.status());
	}

}
