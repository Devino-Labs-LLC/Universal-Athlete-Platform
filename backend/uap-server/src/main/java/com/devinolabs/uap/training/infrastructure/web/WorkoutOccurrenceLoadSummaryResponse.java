package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutOccurrenceLoadSummaryResult;
import com.devinolabs.uap.training.domain.TrainingLoadCalculationVersion;

record WorkoutOccurrenceLoadSummaryResponse(
		UUID id,
		UUID trainingPlanId,
		UUID workoutDayId,
		UUID workoutOccurrenceId,
		LocalDate scheduledDate,
		BigDecimal sessionRpe,
		Integer sessionDurationMinutes,
		BigDecimal sessionRpeLoad,
		TrainingLoadMetricUnit sessionRpeLoadUnit,
		long prescribedExerciseCount,
		long completedExerciseCount,
		long substitutedExerciseCount,
		long completedSetCount,
		long skippedSetCount,
		long completedRepetitionCount,
		BigDecimal totalVolumeKilograms,
		TrainingLoadMetricUnit totalVolumeUnit,
		long totalDurationSeconds,
		TrainingLoadMetricUnit totalDurationUnit,
		BigDecimal totalDistanceMeters,
		TrainingLoadMetricUnit totalDistanceUnit,
		long noImpactExerciseCount,
		long lowImpactExerciseCount,
		long moderateImpactExerciseCount,
		long highImpactExerciseCount,
		List<WorkoutLoadCategorySummaryResponse> categorySummaries,
		List<WorkoutLoadMovementPatternSummaryResponse> movementSummaries,
		Instant calculatedAt,
		Instant sourceUpdatedAt,
		TrainingLoadCalculationVersion calculationVersion) {

	static WorkoutOccurrenceLoadSummaryResponse from(WorkoutOccurrenceLoadSummaryResult result) {
		return new WorkoutOccurrenceLoadSummaryResponse(
				result.id().value(),
				result.trainingPlanId().value(),
				result.workoutDayId().value(),
				result.workoutOccurrenceId().value(),
				result.scheduledDate(),
				result.sessionRpe() == null ? null : result.sessionRpe().value(),
				result.sessionDurationMinutes(),
				result.sessionRpeLoad() == null ? null : result.sessionRpeLoad().value(),
				TrainingLoadMetricUnit.ARBITRARY_UNITS,
				result.prescribedExerciseCount(),
				result.completedExerciseCount(),
				result.substitutedExerciseCount(),
				result.completedSetCount(),
				result.skippedSetCount(),
				result.completedRepetitionCount(),
				result.totalVolumeKilograms(),
				TrainingLoadMetricUnit.KILOGRAM_REPETITIONS,
				result.totalDurationSeconds(),
				TrainingLoadMetricUnit.SECONDS,
				result.totalDistanceMeters(),
				TrainingLoadMetricUnit.METERS,
				result.noImpactExerciseCount(),
				result.lowImpactExerciseCount(),
				result.moderateImpactExerciseCount(),
				result.highImpactExerciseCount(),
				result.categorySummaries().stream().map(WorkoutLoadCategorySummaryResponse::from).toList(),
				result.movementSummaries().stream().map(WorkoutLoadMovementPatternSummaryResponse::from).toList(),
				result.calculatedAt(),
				result.sourceUpdatedAt(),
				result.calculationVersion());
	}

}
