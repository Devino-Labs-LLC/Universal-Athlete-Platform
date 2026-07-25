package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;

public record CreateWorkoutExerciseRequest(
		@NotBlank @Size(max = 160) String exerciseName,
		@NotNull ExerciseCategory category,
		@NotNull ExerciseType type,
		@NotNull @Min(1) Integer sets,
		@Min(1) Integer minimumReps,
		@Min(1) Integer maximumReps,
		@DecimalMin("0") BigDecimal targetWeight,
		WeightUnit weightUnit,
		@Min(0) Integer targetDurationSeconds,
		@DecimalMin("0") BigDecimal targetDistance,
		DistanceUnit distanceUnit,
		@Min(0) Integer targetRestSeconds,
		@Min(0) @Max(10) Integer targetRpe,
		@Size(max = 40) String tempo,
		@Size(max = 2000) String coachingNotes,
		@Min(0) Integer displayOrder) {
}
