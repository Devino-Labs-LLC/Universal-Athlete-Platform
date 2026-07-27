package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutExerciseExecutionJpaRepository extends JpaRepository<WorkoutExerciseExecutionJpaEntity, UUID> {

	List<WorkoutExerciseExecutionJpaEntity> findAllByWorkoutOccurrenceIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
			UUID workoutOccurrenceId);

	@Query("""
			select e from WorkoutExerciseExecutionJpaEntity e
			join WorkoutOccurrenceJpaEntity o on e.workoutOccurrenceId = o.id
			where e.id = :id
			and e.workoutOccurrenceId = :occurrenceId
			and e.athleteId = :athleteId
			and o.workoutDayId = :workoutDayId
			and o.athleteId = :athleteId
			""")
	Optional<WorkoutExerciseExecutionJpaEntity> findOwned(
			@Param("id") UUID id,
			@Param("occurrenceId") UUID occurrenceId,
			@Param("workoutDayId") UUID workoutDayId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select e from WorkoutExerciseExecutionJpaEntity e
			join WorkoutOccurrenceJpaEntity o on e.workoutOccurrenceId = o.id
			where e.workoutOccurrenceId = :occurrenceId
			and e.athleteId = :athleteId
			and o.athleteId = :athleteId
			order by e.displayOrder asc, e.createdAt asc, e.id asc
			""")
	List<WorkoutExerciseExecutionJpaEntity> findAllByOccurrenceIdAndAthleteId(
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select e from WorkoutExerciseExecutionJpaEntity e
			where e.id = :id
			and e.workoutOccurrenceId = :occurrenceId
			and e.athleteId = :athleteId
			""")
	Optional<WorkoutExerciseExecutionJpaEntity> findByIdAndOccurrenceIdAndAthleteId(
			@Param("id") UUID id,
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select e.workoutOccurrenceId, e.status, count(e)
			from WorkoutExerciseExecutionJpaEntity e
			where e.workoutOccurrenceId in :occurrenceIds
			and e.athleteId = :athleteId
			group by e.workoutOccurrenceId, e.status
			""")
	List<Object[]> countByStatusForOccurrences(
			@Param("occurrenceIds") Collection<UUID> occurrenceIds,
			@Param("athleteId") UUID athleteId);

	@Query(value = """
			select e, o.scheduledDate, o.status
			from WorkoutExerciseExecutionJpaEntity e
			join WorkoutOccurrenceJpaEntity o on e.workoutOccurrenceId = o.id
			where e.athleteId = :athleteId
			and e.exercisePerformanceKey = :exercisePerformanceKey
			and e.status = com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus.COMPLETED
			and o.status in (
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.IN_PROGRESS,
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.COMPLETED)
			and (:scheduledFrom is null or o.scheduledDate >= :scheduledFrom)
			and (:scheduledTo is null or o.scheduledDate <= :scheduledTo)
			order by o.scheduledDate desc, e.completedAt desc, e.id desc
			""",
			countQuery = """
			select count(e)
			from WorkoutExerciseExecutionJpaEntity e
			join WorkoutOccurrenceJpaEntity o on e.workoutOccurrenceId = o.id
			where e.athleteId = :athleteId
			and e.exercisePerformanceKey = :exercisePerformanceKey
			and e.status = com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus.COMPLETED
			and o.status in (
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.IN_PROGRESS,
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.COMPLETED)
			and (:scheduledFrom is null or o.scheduledDate >= :scheduledFrom)
			and (:scheduledTo is null or o.scheduledDate <= :scheduledTo)
			""")
	Page<Object[]> findCompletedPerformanceRows(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey,
			@Param("scheduledFrom") LocalDate scheduledFrom,
			@Param("scheduledTo") LocalDate scheduledTo,
			Pageable pageable);

	@Query("""
			select e, o.scheduledDate, o.status
			from WorkoutExerciseExecutionJpaEntity e
			join WorkoutOccurrenceJpaEntity o on e.workoutOccurrenceId = o.id
			where e.athleteId = :athleteId
			and (:exercisePerformanceKey is null or e.exercisePerformanceKey = :exercisePerformanceKey)
			and e.status = com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus.COMPLETED
			and o.status in (
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.IN_PROGRESS,
				com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus.COMPLETED)
			order by o.scheduledDate asc, e.completedAt asc, e.id asc
			""")
	List<Object[]> findEligiblePerformanceRowsChronologically(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

	@Query("""
			select count(e) > 0 from WorkoutExerciseExecutionJpaEntity e
			where e.athleteId = :athleteId
			and e.exercisePerformanceKey = :exercisePerformanceKey
			""")
	boolean existsByAthleteIdAndExercisePerformanceKey(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

}
