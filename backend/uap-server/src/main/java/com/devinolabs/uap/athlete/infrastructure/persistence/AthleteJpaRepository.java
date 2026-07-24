package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AthleteJpaRepository extends JpaRepository<AthleteJpaEntity, UUID> {

	Optional<AthleteJpaEntity> findByAccountId(UUID accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from AthleteJpaEntity a where a.accountId = :accountId")
	Optional<AthleteJpaEntity> findByAccountIdForUpdate(@Param("accountId") UUID accountId);

	boolean existsByAccountId(UUID accountId);

}
