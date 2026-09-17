package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrganizationBillingCustomerJpaRepository
		extends JpaRepository<OrganizationBillingCustomerJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select customer from OrganizationBillingCustomerJpaEntity customer "
			+ "where customer.organizationId = :organizationId")
	Optional<OrganizationBillingCustomerJpaEntity> findByOrganizationIdForUpdate(
			@Param("organizationId") UUID organizationId);

}
