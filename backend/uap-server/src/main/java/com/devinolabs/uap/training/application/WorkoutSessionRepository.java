package com.devinolabs.uap.training.application;

import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutSession;

public interface WorkoutSessionRepository {

	WorkoutSession save(WorkoutSession session);

	Optional<WorkoutSession> findByWorkoutExerciseIdAndAthleteId(
			WorkoutExerciseId workoutExerciseId,
			AthleteId athleteId);

	Optional<WorkoutSession> findByWorkoutExerciseIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseId workoutExerciseId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId);

	boolean existsByWorkoutExerciseId(WorkoutExerciseId workoutExerciseId);

}
