package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;

public record UpdateWorkoutSessionRequest(
		PatchValue<Integer> actualSets,
		PatchValue<Integer> actualReps,
		PatchValue<BigDecimal> actualWeight,
		PatchValue<WeightUnit> weightUnit,
		PatchValue<Integer> actualDurationSeconds,
		PatchValue<BigDecimal> actualDistance,
		PatchValue<DistanceUnit> distanceUnit,
		PatchValue<Integer> actualRestSeconds,
		PatchValue<Integer> actualRpe,
		PatchValue<String> athleteNotes) {
}
