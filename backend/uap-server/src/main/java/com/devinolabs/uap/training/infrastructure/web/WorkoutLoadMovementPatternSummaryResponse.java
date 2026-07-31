package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.WorkoutLoadMovementPatternSummary;

record WorkoutLoadMovementPatternSummaryResponse(
		MovementPattern primaryMovementPattern,
		long completedExerciseCount,
		long completedSetCount,
		long completedRepetitionCount,
		BigDecimal volumeKilograms,
		TrainingLoadMetricUnit volumeUnit,
		long durationSeconds,
		TrainingLoadMetricUnit durationUnit,
		BigDecimal distanceMeters,
		TrainingLoadMetricUnit distanceUnit) {

	static WorkoutLoadMovementPatternSummaryResponse from(WorkoutLoadMovementPatternSummary summary) {
		return new WorkoutLoadMovementPatternSummaryResponse(
				summary.primaryMovementPattern(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.completedRepetitionCount(),
				summary.volumeKilograms(),
				TrainingLoadMetricUnit.KILOGRAM_REPETITIONS,
				summary.durationSeconds(),
				TrainingLoadMetricUnit.SECONDS,
				summary.distanceMeters(),
				TrainingLoadMetricUnit.METERS);
	}

}
