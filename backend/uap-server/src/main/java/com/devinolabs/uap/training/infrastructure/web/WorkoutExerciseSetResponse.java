package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

public record WorkoutExerciseSetResponse(
		UUID id,
		UUID workoutExerciseExecutionId,
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
