package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * A completed execution joined to the occurrence that scheduled it, which is where performance
 * history gets its date and its occurrence-level eligibility.
 */
public record ExercisePerformanceExecutionRow(
		WorkoutExerciseExecution execution,
		LocalDate scheduledDate,
		WorkoutOccurrenceStatus occurrenceStatus) {

	public ExercisePerformanceExecutionRow {
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		Objects.requireNonNull(occurrenceStatus, "occurrenceStatus must not be null");
	}

}
