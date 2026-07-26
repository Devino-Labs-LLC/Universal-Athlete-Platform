package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;

public record UpdateWorkoutExerciseExecutionCommand(
		Integer actualSets,
		boolean actualSetsPresent,
		Integer actualReps,
		boolean actualRepsPresent,
		BigDecimal actualWeight,
		boolean actualWeightPresent,
		WeightUnit weightUnit,
		boolean weightUnitPresent,
		Integer actualDurationSeconds,
		boolean actualDurationSecondsPresent,
		BigDecimal actualDistance,
		boolean actualDistancePresent,
		DistanceUnit distanceUnit,
		boolean distanceUnitPresent,
		Integer actualRestSeconds,
		boolean actualRestSecondsPresent,
		Integer actualRpe,
		boolean actualRpePresent,
		String athleteNotes,
		boolean athleteNotesPresent) {
}
