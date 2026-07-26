package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

/**
 * Prescription for a new set. When {@code copyFromSetId} is present the prescription is taken from
 * that sibling set (prescription only, never its actuals) and the explicit fields are ignored.
 */
public record AddWorkoutExerciseSetCommand(
		WorkoutExerciseSetId copyFromSetId,
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
