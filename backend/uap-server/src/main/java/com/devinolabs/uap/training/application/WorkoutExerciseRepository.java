package com.devinolabs.uap.training.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

public interface WorkoutExerciseRepository {

	WorkoutExercise save(WorkoutExercise exercise);

	List<WorkoutExercise> saveAll(Collection<WorkoutExercise> exercises);

	Optional<WorkoutExercise> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId);

	List<WorkoutExercise> findAllByWorkoutDayIdAndAthleteId(WorkoutDayId workoutDayId, AthleteId athleteId);

	boolean existsByWorkoutDayIdAndNormalizedExerciseName(WorkoutDayId workoutDayId, String normalizedExerciseName);

	boolean existsByWorkoutDayIdAndNormalizedExerciseNameExcluding(
			WorkoutDayId workoutDayId,
			String normalizedExerciseName,
			WorkoutExerciseId excludingId);

	int findMaxDisplayOrder(WorkoutDayId workoutDayId, AthleteId athleteId);

	List<WorkoutExercise> findAllByWorkoutDayIdWithDisplayOrderAtLeast(
			WorkoutDayId workoutDayId,
			int displayOrder);

	void delete(WorkoutExercise exercise);

}
