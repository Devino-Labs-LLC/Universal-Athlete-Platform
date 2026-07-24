package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;

interface AssessmentJpaRepository extends JpaRepository<AssessmentJpaEntity, UUID> {

	Optional<AssessmentJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	@Query("""
			select a from AssessmentJpaEntity a
			where a.athleteId = :athleteId
			and (:status is null or a.status = :status)
			and (:assessmentType is null or a.assessmentType = :assessmentType)
			and (:scheduledFrom is null or a.scheduledAt >= :scheduledFrom)
			and (:scheduledTo is null or a.scheduledAt <= :scheduledTo)
			""")
	List<AssessmentJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("status") AssessmentStatus status,
			@Param("assessmentType") AssessmentType assessmentType,
			@Param("scheduledFrom") Instant scheduledFrom,
			@Param("scheduledTo") Instant scheduledTo);

	@Query("""
			select case when count(a) > 0 then true else false end
			from AssessmentJpaEntity a
			where a.athleteId = :athleteId
			and a.assessmentType = :assessmentType
			and a.normalizedTitle = :normalizedTitle
			and a.status <> com.devinolabs.uap.athlete.domain.AssessmentStatus.CANCELLED
			and (
				(:scheduledAt is null and a.scheduledAt is null)
				or a.scheduledAt = :scheduledAt
			)
			and (:excludingId is null or a.id <> :excludingId)
			""")
	boolean existsDuplicate(
			@Param("athleteId") UUID athleteId,
			@Param("assessmentType") AssessmentType assessmentType,
			@Param("normalizedTitle") String normalizedTitle,
			@Param("scheduledAt") Instant scheduledAt,
			@Param("excludingId") UUID excludingId);

}
