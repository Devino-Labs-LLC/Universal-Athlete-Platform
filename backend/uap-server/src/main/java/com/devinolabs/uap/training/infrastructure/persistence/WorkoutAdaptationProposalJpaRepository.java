package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

interface WorkoutAdaptationProposalJpaRepository extends JpaRepository<WorkoutAdaptationProposalJpaEntity, UUID> {

	@Query("""
			select distinct p from WorkoutAdaptationProposalJpaEntity p
			left join fetch p.items
			where p.id = :id and p.athleteId = :athleteId
			""")
	Optional<WorkoutAdaptationProposalJpaEntity> findOwnedById(
			@Param("id") UUID id,
			@Param("athleteId") UUID athleteId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select distinct p from WorkoutAdaptationProposalJpaEntity p
			left join fetch p.items
			where p.id = :id and p.athleteId = :athleteId
			""")
	Optional<WorkoutAdaptationProposalJpaEntity> lockOwnedById(
			@Param("id") UUID id,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select distinct p from WorkoutAdaptationProposalJpaEntity p
			left join fetch p.items
			where p.workoutOccurrenceId = :occurrenceId
			and p.athleteId = :athleteId
			and p.status in :activeStatuses
			""")
	List<WorkoutAdaptationProposalJpaEntity> findActiveByOccurrenceId(
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId,
			@Param("activeStatuses") List<WorkoutAdaptationProposalStatus> activeStatuses);

	@Query("""
			select p.id, p.workoutOccurrenceId, p.status, p.origin, p.unresolvedExecutions
			from WorkoutAdaptationProposalJpaEntity p
			where p.workoutOccurrenceId = :occurrenceId
			and p.athleteId = :athleteId
			and p.status in :activeStatuses
			order by p.generatedAt desc, p.id asc
			""")
	List<Object[]> findActiveBriefRowsByOccurrenceId(
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId,
			@Param("activeStatuses") List<WorkoutAdaptationProposalStatus> activeStatuses);

	@Query("""
			select p.id, p.workoutOccurrenceId, p.status, p.unresolvedExecutions, p.generatedAt, p.expiresAt
			from WorkoutAdaptationProposalJpaEntity p
			where p.athleteId = :athleteId
			and p.status in :statuses
			order by p.generatedAt desc, p.id asc
			""")
	List<Object[]> findOutstandingBriefRowsByAthlete(
			@Param("athleteId") UUID athleteId,
			@Param("statuses") List<WorkoutAdaptationProposalStatus> statuses,
			Pageable pageable);

	@Query(
			value = """
					select p from WorkoutAdaptationProposalJpaEntity p
					where p.athleteId = :athleteId
					and (:occurrenceId is null or p.workoutOccurrenceId = :occurrenceId)
					and (:status is null or p.status = :status)
					order by p.generatedAt desc, p.id asc
					""",
			countQuery = """
					select count(p) from WorkoutAdaptationProposalJpaEntity p
					where p.athleteId = :athleteId
					and (:occurrenceId is null or p.workoutOccurrenceId = :occurrenceId)
					and (:status is null or p.status = :status)
					""")
	Page<WorkoutAdaptationProposalJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("occurrenceId") UUID occurrenceId,
			@Param("status") WorkoutAdaptationProposalStatus status,
			Pageable pageable);

}
