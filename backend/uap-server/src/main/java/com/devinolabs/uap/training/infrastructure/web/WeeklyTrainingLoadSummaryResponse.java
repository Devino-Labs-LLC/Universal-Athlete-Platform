package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;

record WeeklyTrainingLoadSummaryResponse(
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		long occurrenceCount,
		long trainingDays,
		long ratedOccurrenceCount,
		long unratedOccurrenceCount,
		long completedExerciseCount,
		long completedSetCount,
		long completedRepetitionCount,
		BigDecimal totalVolumeKilograms,
		TrainingLoadMetricUnit totalVolumeUnit,
		long totalDurationSeconds,
		TrainingLoadMetricUnit totalDurationUnit,
		BigDecimal totalDistanceMeters,
		TrainingLoadMetricUnit totalDistanceUnit,
		BigDecimal totalSessionRpeLoad,
		TrainingLoadMetricUnit totalSessionRpeLoadUnit,
		BigDecimal averageSessionRpe,
		long totalSessionDurationMinutes,
		BigDecimal highestSessionRpe,
		long noImpactExerciseCount,
		long lowImpactExerciseCount,
		long moderateImpactExerciseCount,
		long highImpactExerciseCount,
		List<WorkoutLoadCategorySummaryResponse> categorySummaries,
		List<WorkoutLoadMovementPatternSummaryResponse> movementSummaries) {

	static WeeklyTrainingLoadSummaryResponse from(WeeklyTrainingLoadSummary summary) {
		return new WeeklyTrainingLoadSummaryResponse(
				summary.weekStartDate(),
				summary.weekEndDate(),
				summary.occurrenceCount(),
				summary.trainingDays(),
				summary.ratedOccurrenceCount(),
				summary.unratedOccurrenceCount(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.completedRepetitionCount(),
				summary.totalVolumeKilograms(),
				TrainingLoadMetricUnit.KILOGRAM_REPETITIONS,
				summary.totalDurationSeconds(),
				TrainingLoadMetricUnit.SECONDS,
				summary.totalDistanceMeters(),
				TrainingLoadMetricUnit.METERS,
				summary.totalSessionRpeLoad(),
				TrainingLoadMetricUnit.ARBITRARY_UNITS,
				summary.averageSessionRpe(),
				summary.totalSessionDurationMinutes(),
				summary.highestSessionRpe(),
				summary.noImpactExerciseCount(),
				summary.lowImpactExerciseCount(),
				summary.moderateImpactExerciseCount(),
				summary.highImpactExerciseCount(),
				summary.categorySummaries().stream().map(WorkoutLoadCategorySummaryResponse::from).toList(),
				summary.movementSummaries().stream().map(WorkoutLoadMovementPatternSummaryResponse::from).toList());
	}

}
