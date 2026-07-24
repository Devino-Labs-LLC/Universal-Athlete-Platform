package com.devinolabs.uap.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionJpaEntity, UUID> {

	Optional<RefreshSessionJpaEntity> findByTokenDigest(String tokenDigest);

	@Query("""
			select s from RefreshSessionJpaEntity s
			where s.accountId = :accountId
			  and s.revokedAt is null
			  and s.expiresAt > :now
			""")
	List<RefreshSessionJpaEntity> findActiveByAccountId(@Param("accountId") UUID accountId, @Param("now") Instant now);

}
