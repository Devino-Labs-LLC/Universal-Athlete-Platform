package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

public record WorkoutExerciseSetResult(
		WorkoutExerciseSetId id,
		WorkoutExerciseExecutionId workoutExerciseExecutionId,
		int setNumber,
		int displayOrder,
		WorkoutExerciseSetType setType,
		Integer prescribedMinimumReps,
		Integer prescribedMaximumReps,
		BigDecimal prescribedWeight,
		WeightUnit prescribedWeightUnit,
		Integer prescribedDurationSeconds,
		BigDecimal prescribedDistance,
		DistanceUnit prescribedDistanceUnit,
		Integer prescribedTargetRpe,
		Integer prescribedRestSeconds,
		Integer actualReps,
		BigDecimal actualWeight,
		WeightUnit actualWeightUnit,
		Integer actualDurationSeconds,
		BigDecimal actualDistance,
		DistanceUnit actualDistanceUnit,
		Integer actualRestSeconds,
		BigDecimal actualRpe,
		WorkoutExerciseSetStatus status,
		Instant startedAt,
		Instant completedAt,
		String athleteNotes,
		Instant createdAt,
		Instant updatedAt) {
}
