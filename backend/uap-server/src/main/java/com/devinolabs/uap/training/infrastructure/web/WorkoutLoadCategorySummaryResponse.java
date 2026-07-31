package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.WorkoutLoadCategorySummary;

record WorkoutLoadCategorySummaryResponse(
		ExerciseDefinitionCategory category,
		long completedExerciseCount,
		long completedSetCount,
		BigDecimal volumeKilograms,
		TrainingLoadMetricUnit volumeUnit,
		long durationSeconds,
		TrainingLoadMetricUnit durationUnit,
		BigDecimal distanceMeters,
		TrainingLoadMetricUnit distanceUnit) {

	static WorkoutLoadCategorySummaryResponse from(WorkoutLoadCategorySummary summary) {
		return new WorkoutLoadCategorySummaryResponse(
				summary.category(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.volumeKilograms(),
				TrainingLoadMetricUnit.KILOGRAM_REPETITIONS,
				summary.durationSeconds(),
				TrainingLoadMetricUnit.SECONDS,
				summary.distanceMeters(),
				TrainingLoadMetricUnit.METERS);
	}

}
