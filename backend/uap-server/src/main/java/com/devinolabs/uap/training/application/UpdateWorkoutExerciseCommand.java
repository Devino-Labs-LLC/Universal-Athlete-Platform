package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;

public record UpdateWorkoutExerciseCommand(
		ExerciseDefinitionId exerciseDefinitionId,
		boolean exerciseDefinitionIdPresent,
		String exerciseName,
		boolean exerciseNamePresent,
		ExerciseCategory category,
		boolean categoryPresent,
		ExerciseType type,
		boolean typePresent,
		Integer sets,
		boolean setsPresent,
		Integer minimumReps,
		boolean minimumRepsPresent,
		Integer maximumReps,
		boolean maximumRepsPresent,
		BigDecimal targetWeight,
		boolean targetWeightPresent,
		WeightUnit weightUnit,
		boolean weightUnitPresent,
		Integer targetDurationSeconds,
		boolean targetDurationSecondsPresent,
		BigDecimal targetDistance,
		boolean targetDistancePresent,
		DistanceUnit distanceUnit,
		boolean distanceUnitPresent,
		Integer targetRestSeconds,
		boolean targetRestSecondsPresent,
		Integer targetRpe,
		boolean targetRpePresent,
		String tempo,
		boolean tempoPresent,
		String coachingNotes,
		boolean coachingNotesPresent,
		Integer displayOrder,
		boolean displayOrderPresent) {
}
