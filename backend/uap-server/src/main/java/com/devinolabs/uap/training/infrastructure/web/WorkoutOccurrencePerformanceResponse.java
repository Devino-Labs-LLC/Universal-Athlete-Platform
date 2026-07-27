package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutOccurrencePerformanceResult;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

record WorkoutOccurrencePerformanceResponse(
		UUID occurrenceId,
		LocalDate scheduledDate,
		WorkoutOccurrenceStatus status,
		Instant startedAt,
		Instant completedAt,
		TotalsResponse totals,
		List<ExerciseExecutionPerformanceResponse> exercises) {

	record TotalsResponse(
			int completedExerciseCount,
			int completedSetCount,
			Integer totalRepetitions,
			BigDecimal totalVolumeKilogramRepetitions,
			Integer totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal averageRpe) {
	}

	static WorkoutOccurrencePerformanceResponse from(WorkoutOccurrencePerformanceResult result) {
		WorkoutOccurrencePerformanceResult.Totals totals = result.totals();
		return new WorkoutOccurrencePerformanceResponse(
				result.occurrenceId().value(),
				result.scheduledDate(),
				result.status(),
				result.startedAt(),
				result.completedAt(),
				new TotalsResponse(
						totals.completedExerciseCount(),
						totals.completedSetCount(),
						totals.totalRepetitions(),
						totals.totalVolumeKilogramRepetitions(),
						totals.totalDurationSeconds(),
						totals.totalDistanceMeters(),
						totals.averageRpe()),
				result.exercises().stream().map(ExerciseExecutionPerformanceResponse::from).toList());
	}

}
