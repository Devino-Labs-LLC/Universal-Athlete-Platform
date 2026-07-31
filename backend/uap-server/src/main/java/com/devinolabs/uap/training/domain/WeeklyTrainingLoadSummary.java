package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyTrainingLoadSummary(
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
		long totalDurationSeconds,
		BigDecimal totalDistanceMeters,
		BigDecimal totalSessionRpeLoad,
		BigDecimal averageSessionRpe,
		long totalSessionDurationMinutes,
		BigDecimal highestSessionRpe,
		long noImpactExerciseCount,
		long lowImpactExerciseCount,
		long moderateImpactExerciseCount,
		long highImpactExerciseCount,
		List<WorkoutLoadCategorySummary> categorySummaries,
		List<WorkoutLoadMovementPatternSummary> movementSummaries) {
}
