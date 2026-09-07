package com.devinolabs.uap.identity.infrastructure.directory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.identity.api.AccountDirectoryPort;
import com.devinolabs.uap.identity.api.AccountDirectoryRef;
import com.devinolabs.uap.identity.application.AccountRepository;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;

@Component
class AccountDirectoryPortAdapter implements AccountDirectoryPort {

	private final AccountRepository accountRepository;

	AccountDirectoryPortAdapter(AccountRepository accountRepository) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
	}

	@Override
	public Optional<AccountDirectoryRef> findByNormalizedEmail(String email) {
		Objects.requireNonNull(email, "email must not be null");
		EmailAddress normalized = EmailAddress.of(email);
		return accountRepository.findByEmail(normalized).map(this::toRef);
	}

	@Override
	public Optional<AccountDirectoryRef> findByAccountId(UUID accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		return accountRepository.findById(AccountId.of(accountId)).map(this::toRef);
	}

	private AccountDirectoryRef toRef(Account account) {
		return new AccountDirectoryRef(
				account.id().value(),
				account.email().value(),
				account.emailVerifiedAt() != null);
	}

}
