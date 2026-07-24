package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AthleteJpaRepository extends JpaRepository<AthleteJpaEntity, UUID> {

	Optional<AthleteJpaEntity> findByAccountId(UUID accountId);

}
