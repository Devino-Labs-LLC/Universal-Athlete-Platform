package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

public record UpdateWorkoutExerciseSetRequest(
		PatchValue<WorkoutExerciseSetType> setType,
		PatchValue<Integer> actualReps,
		PatchValue<BigDecimal> actualWeight,
		PatchValue<WeightUnit> actualWeightUnit,
		PatchValue<Integer> actualDurationSeconds,
		PatchValue<BigDecimal> actualDistance,
		PatchValue<DistanceUnit> actualDistanceUnit,
		PatchValue<Integer> actualRestSeconds,
		PatchValue<BigDecimal> actualRpe,
		PatchValue<String> athleteNotes) {
}
