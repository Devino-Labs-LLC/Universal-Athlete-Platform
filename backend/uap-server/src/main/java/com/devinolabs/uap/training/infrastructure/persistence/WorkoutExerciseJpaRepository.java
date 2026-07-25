package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutExerciseJpaRepository extends JpaRepository<WorkoutExerciseJpaEntity, UUID> {

	Optional<WorkoutExerciseJpaEntity> findByIdAndWorkoutDayIdAndAthleteId(
			UUID id,
			UUID workoutDayId,
			UUID athleteId);

	List<WorkoutExerciseJpaEntity> findAllByWorkoutDayIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
			UUID workoutDayId,
			UUID athleteId);

	boolean existsByWorkoutDayIdAndNormalizedExerciseName(UUID workoutDayId, String normalizedExerciseName);

	boolean existsByWorkoutDayIdAndNormalizedExerciseNameAndIdNot(
			UUID workoutDayId,
			String normalizedExerciseName,
			UUID id);

	@Query("""
			select coalesce(max(e.displayOrder), -1)
			from WorkoutExerciseJpaEntity e
			where e.workoutDayId = :workoutDayId
			and e.athleteId = :athleteId
			""")
	int findMaxDisplayOrder(@Param("workoutDayId") UUID workoutDayId, @Param("athleteId") UUID athleteId);

	List<WorkoutExerciseJpaEntity> findAllByWorkoutDayIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderDesc(
			UUID workoutDayId,
			int displayOrder);

}
