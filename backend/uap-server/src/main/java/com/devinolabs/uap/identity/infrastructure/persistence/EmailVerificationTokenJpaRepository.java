package com.devinolabs.uap.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpaEntity, UUID> {

	Optional<EmailVerificationTokenJpaEntity> findByTokenDigest(String tokenDigest);

}
