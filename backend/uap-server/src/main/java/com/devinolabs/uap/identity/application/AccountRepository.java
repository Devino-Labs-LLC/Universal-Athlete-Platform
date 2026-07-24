package com.devinolabs.uap.identity.application;

import java.util.Optional;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(AccountId id);

	Optional<Account> findByEmail(EmailAddress email);

	boolean existsByEmail(EmailAddress email);

}
