package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SignalFamily;

interface IntegrationEvidenceJpaRepository extends JpaRepository<IntegrationEvidenceJpaEntity, UUID> {

	Optional<IntegrationEvidenceJpaEntity> findByProviderAndAthleteIdAndExternalRecordId(
			HealthProviderKey provider,
			UUID athleteId,
			String externalRecordId);

	@Query("""
			select e from IntegrationEvidenceJpaEntity e
			where e.athleteId = :athleteId
			  and e.observedAt >= :fromInclusive
			  and e.observedAt <= :toInclusive
			  and (:family is null or e.signalFamily = :family)
			  and e.status = com.devinolabs.uap.integrations.domain.EvidenceStatus.ACTIVE
			order by e.observedAt asc
			""")
	List<IntegrationEvidenceJpaEntity> findActiveByAthleteAndWindow(
			@Param("athleteId") UUID athleteId,
			@Param("fromInclusive") Instant fromInclusive,
			@Param("toInclusive") Instant toInclusive,
			@Param("family") SignalFamily family);

	@Query("""
			select e from IntegrationEvidenceJpaEntity e
			where e.connectionId = :connectionId
			  and e.observedAt >= :fromInclusive
			  and e.observedAt <= :toInclusive
			  and (:family is null or e.signalFamily = :family)
			  and e.status = com.devinolabs.uap.integrations.domain.EvidenceStatus.ACTIVE
			order by e.observedAt asc
			""")
	List<IntegrationEvidenceJpaEntity> findActiveByConnectionAndWindow(
			@Param("connectionId") UUID connectionId,
			@Param("fromInclusive") Instant fromInclusive,
			@Param("toInclusive") Instant toInclusive,
			@Param("family") SignalFamily family);

	long countByConnectionId(UUID connectionId);
}
