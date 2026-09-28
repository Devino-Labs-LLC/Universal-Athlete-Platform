package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AccountBillingCustomerJpaRepository
		extends JpaRepository<AccountBillingCustomerJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select customer from AccountBillingCustomerJpaEntity customer "
			+ "where customer.accountId = :accountId")
	Optional<AccountBillingCustomerJpaEntity> findByAccountIdForUpdate(
			@Param("accountId") UUID accountId);

}
