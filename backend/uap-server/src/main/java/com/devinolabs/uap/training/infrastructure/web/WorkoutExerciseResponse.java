package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseStatus;

public record WorkoutExerciseResponse(
		java.util.UUID id,
		int displayOrder,
		String exerciseName,
		ExerciseCategory category,
		ExerciseType type,
		Integer sets,
		Integer minimumReps,
		Integer maximumReps,
		BigDecimal targetWeight,
		WeightUnit weightUnit,
		Integer targetDurationSeconds,
		BigDecimal targetDistance,
		DistanceUnit distanceUnit,
		Integer targetRestSeconds,
		Integer targetRpe,
		String tempo,
		String coachingNotes,
		WorkoutExerciseStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
