package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.billing.application.AccountBillingCustomerRepository;
import com.devinolabs.uap.billing.domain.AccountBillingCustomer;

@Repository
class JpaAccountBillingCustomerRepository implements AccountBillingCustomerRepository {

	private final AccountBillingCustomerJpaRepository jpaRepository;

	JpaAccountBillingCustomerRepository(AccountBillingCustomerJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AccountBillingCustomer save(AccountBillingCustomer customer) {
		AccountBillingCustomerJpaEntity entity = new AccountBillingCustomerJpaEntity(
				customer.accountId(),
				customer.provider(),
				customer.providerCustomerRef(),
				customer.createdAt(),
				customer.updatedAt());
		return toDomain(jpaRepository.saveAndFlush(entity));
	}

	@Override
	public Optional<AccountBillingCustomer> findByAccountId(UUID accountId) {
		return jpaRepository.findById(accountId).map(JpaAccountBillingCustomerRepository::toDomain);
	}

	@Override
	public Optional<AccountBillingCustomer> findByAccountIdForUpdate(UUID accountId) {
		return jpaRepository.findByAccountIdForUpdate(accountId)
				.map(JpaAccountBillingCustomerRepository::toDomain);
	}

	private static AccountBillingCustomer toDomain(AccountBillingCustomerJpaEntity entity) {
		return new AccountBillingCustomer(
				entity.getId(),
				entity.provider(),
				entity.providerCustomerRef(),
				entity.createdAt(),
				entity.updatedAt());
	}

}
