package com.devinolabs.uap.identity.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;

@Service
public class GetCurrentAccountUseCase {

	private final AccountRepository accountRepository;

	public GetCurrentAccountUseCase(AccountRepository accountRepository) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
	}

	@Transactional(readOnly = true)
	public CurrentAccountResult execute(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Account account = accountRepository.findById(accountId)
				.orElseThrow(AccountNotFoundException::new);
		return new CurrentAccountResult(
				account.id(),
				account.email(),
				account.status(),
				account.emailVerifiedAt());
	}

}
