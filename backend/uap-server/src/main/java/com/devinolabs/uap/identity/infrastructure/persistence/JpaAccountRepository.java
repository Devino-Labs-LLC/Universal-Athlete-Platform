package com.devinolabs.uap.identity.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.identity.application.AccountRepository;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;

@Repository
class JpaAccountRepository implements AccountRepository {

	private final AccountJpaRepository jpaRepository;

	JpaAccountRepository(AccountJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository, "AccountJpaRepository must not be null");
	}

	@Override
	public Account save(Account account) {
		boolean isNew = !jpaRepository.existsById(account.id().value());
		AccountJpaEntity saved = jpaRepository.save(AccountPersistenceMapper.toEntity(account, isNew));
		return AccountPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Account> findById(AccountId id) {
		return jpaRepository.findById(id.value()).map(AccountPersistenceMapper::toDomain);
	}

	@Override
	public Optional<Account> findByEmail(EmailAddress email) {
		return jpaRepository.findByEmail(email.value()).map(AccountPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByEmail(EmailAddress email) {
		return jpaRepository.existsByEmail(email.value());
	}

}
