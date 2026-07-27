package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record WorkoutOccurrencePerformanceResult(
		WorkoutOccurrenceId occurrenceId,
		LocalDate scheduledDate,
		WorkoutOccurrenceStatus status,
		Instant startedAt,
		Instant completedAt,
		Totals totals,
		List<ExerciseExecutionPerformanceResult> exercises) {

	/**
	 * Occurrence-wide roll-up. Volume and distance are in canonical units because exercises inside
	 * one session may have been logged in different units.
	 */
	public record Totals(
			int completedExerciseCount,
			int completedSetCount,
			Integer totalRepetitions,
			BigDecimal totalVolumeKilogramRepetitions,
			Integer totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal averageRpe) {
	}

}
