package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

public record UpdateWorkoutExerciseSetCommand(
		WorkoutExerciseSetType setType,
		boolean setTypePresent,
		Integer actualReps,
		boolean actualRepsPresent,
		BigDecimal actualWeight,
		boolean actualWeightPresent,
		WeightUnit actualWeightUnit,
		boolean actualWeightUnitPresent,
		Integer actualDurationSeconds,
		boolean actualDurationSecondsPresent,
		BigDecimal actualDistance,
		boolean actualDistancePresent,
		DistanceUnit actualDistanceUnit,
		boolean actualDistanceUnitPresent,
		Integer actualRestSeconds,
		boolean actualRestSecondsPresent,
		BigDecimal actualRpe,
		boolean actualRpePresent,
		String athleteNotes,
		boolean athleteNotesPresent) {

	boolean touchesActuals() {
		return actualRepsPresent
				|| actualWeightPresent
				|| actualWeightUnitPresent
				|| actualDurationSecondsPresent
				|| actualDistancePresent
				|| actualDistanceUnitPresent
				|| actualRestSecondsPresent
				|| actualRpePresent;
	}

}
