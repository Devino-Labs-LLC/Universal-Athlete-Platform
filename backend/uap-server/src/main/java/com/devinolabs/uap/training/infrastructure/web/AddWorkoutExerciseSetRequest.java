package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

public record AddWorkoutExerciseSetRequest(
		UUID copyFromSetId,
		WorkoutExerciseSetType setType,
		Integer prescribedMinimumReps,
		Integer prescribedMaximumReps,
		BigDecimal prescribedWeight,
		WeightUnit prescribedWeightUnit,
		Integer prescribedDurationSeconds,
		BigDecimal prescribedDistance,
		DistanceUnit prescribedDistanceUnit,
		Integer prescribedTargetRpe,
		Integer prescribedRestSeconds) {
}
