package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkoutSessionJpaRepository extends JpaRepository<WorkoutSessionJpaEntity, UUID> {

	Optional<WorkoutSessionJpaEntity> findByWorkoutExerciseIdAndAthleteId(UUID workoutExerciseId, UUID athleteId);

	Optional<WorkoutSessionJpaEntity> findByWorkoutExerciseIdAndWorkoutDayIdAndAthleteId(
			UUID workoutExerciseId,
			UUID workoutDayId,
			UUID athleteId);

	boolean existsByWorkoutExerciseId(UUID workoutExerciseId);

}
