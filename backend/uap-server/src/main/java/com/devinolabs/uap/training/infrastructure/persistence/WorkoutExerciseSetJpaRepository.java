package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutExerciseSetJpaRepository extends JpaRepository<WorkoutExerciseSetJpaEntity, UUID> {

	@Query("""
			select s from WorkoutExerciseSetJpaEntity s
			where s.id = :id
			and s.workoutExerciseExecutionId = :executionId
			and s.athleteId = :athleteId
			""")
	Optional<WorkoutExerciseSetJpaEntity> findOwned(
			@Param("id") UUID id,
			@Param("executionId") UUID executionId,
			@Param("athleteId") UUID athleteId);

	List<WorkoutExerciseSetJpaEntity>
			findAllByWorkoutExerciseExecutionIdAndAthleteIdOrderByDisplayOrderAscSetNumberAscIdAsc(
					UUID workoutExerciseExecutionId,
					UUID athleteId);

	List<WorkoutExerciseSetJpaEntity>
			findAllByWorkoutOccurrenceIdAndAthleteIdOrderByDisplayOrderAscSetNumberAscIdAsc(
					UUID workoutOccurrenceId,
					UUID athleteId);

	@Query("""
			select s.workoutExerciseExecutionId, s.status, count(s)
			from WorkoutExerciseSetJpaEntity s
			where s.workoutExerciseExecutionId in :executionIds
			and s.athleteId = :athleteId
			group by s.workoutExerciseExecutionId, s.status
			""")
	List<Object[]> countByStatusForExecutions(
			@Param("executionIds") Collection<UUID> executionIds,
			@Param("athleteId") UUID athleteId);

}
