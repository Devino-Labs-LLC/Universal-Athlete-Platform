package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

}
