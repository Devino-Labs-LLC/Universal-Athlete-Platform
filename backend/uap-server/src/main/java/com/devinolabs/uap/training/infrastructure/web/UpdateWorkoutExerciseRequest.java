package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;

public record UpdateWorkoutExerciseRequest(
		PatchValue<String> exerciseName,
		PatchValue<ExerciseCategory> category,
		PatchValue<ExerciseType> type,
		PatchValue<Integer> sets,
		PatchValue<Integer> minimumReps,
		PatchValue<Integer> maximumReps,
		PatchValue<BigDecimal> targetWeight,
		PatchValue<WeightUnit> weightUnit,
		PatchValue<Integer> targetDurationSeconds,
		PatchValue<BigDecimal> targetDistance,
		PatchValue<DistanceUnit> distanceUnit,
		PatchValue<Integer> targetRestSeconds,
		PatchValue<Integer> targetRpe,
		PatchValue<String> tempo,
		PatchValue<String> coachingNotes,
		PatchValue<Integer> displayOrder) {
}
