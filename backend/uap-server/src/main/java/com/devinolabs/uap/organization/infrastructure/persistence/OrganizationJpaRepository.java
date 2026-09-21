package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from OrganizationJpaEntity o where o.id = :id")
	Optional<OrganizationJpaEntity> findByIdForUpdate(@Param("id") UUID id);

}
