package com.devinolabs.uap.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

	Optional<AccountJpaEntity> findByEmail(String email);

	boolean existsByEmail(String email);

}
