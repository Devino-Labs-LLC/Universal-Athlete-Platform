package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.SessionRpeLoad;
import com.devinolabs.uap.training.domain.TrainingLoadCalculationVersion;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutLoadCategorySummary;
import com.devinolabs.uap.training.domain.WorkoutLoadMovementPatternSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummaryId;
import com.devinolabs.uap.training.domain.TrainingPlanId;

public record WorkoutOccurrenceLoadSummaryResult(
		WorkoutOccurrenceLoadSummaryId id,
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		WorkoutOccurrenceId workoutOccurrenceId,
		LocalDate scheduledDate,
		SessionRpe sessionRpe,
		Integer sessionDurationMinutes,
		SessionRpeLoad sessionRpeLoad,
		long prescribedExerciseCount,
		long completedExerciseCount,
		long substitutedExerciseCount,
		long completedSetCount,
		long skippedSetCount,
		long completedRepetitionCount,
		BigDecimal totalVolumeKilograms,
		long totalDurationSeconds,
		BigDecimal totalDistanceMeters,
		long noImpactExerciseCount,
		long lowImpactExerciseCount,
		long moderateImpactExerciseCount,
		long highImpactExerciseCount,
		List<WorkoutLoadCategorySummary> categorySummaries,
		List<WorkoutLoadMovementPatternSummary> movementSummaries,
		Instant calculatedAt,
		Instant sourceUpdatedAt,
		TrainingLoadCalculationVersion calculationVersion) {

	public static WorkoutOccurrenceLoadSummaryResult from(WorkoutOccurrenceLoadSummary summary) {
		return new WorkoutOccurrenceLoadSummaryResult(
				summary.id(),
				summary.trainingPlanId(),
				summary.workoutDayId(),
				summary.workoutOccurrenceId(),
				summary.scheduledDate(),
				summary.sessionRpe(),
				summary.sessionDurationMinutes(),
				summary.sessionRpeLoad(),
				summary.prescribedExerciseCount(),
				summary.completedExerciseCount(),
				summary.substitutedExerciseCount(),
				summary.completedSetCount(),
				summary.skippedSetCount(),
				summary.completedRepetitionCount(),
				summary.totalVolumeKilograms(),
				summary.totalDurationSeconds(),
				summary.totalDistanceMeters(),
				summary.noImpactExerciseCount(),
				summary.lowImpactExerciseCount(),
				summary.moderateImpactExerciseCount(),
				summary.highImpactExerciseCount(),
				summary.categorySummaries(),
				summary.movementSummaries(),
				summary.calculatedAt(),
				summary.sourceUpdatedAt(),
				summary.calculationVersion());
	}
}
