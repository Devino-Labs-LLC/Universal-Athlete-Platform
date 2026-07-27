package com.devinolabs.uap.training.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Where a personal record came from. Provenance is derived, so the identifiers are kept as plain
 * references rather than as owning relationships.
 */
public record PersonalRecordProvenance(
		String exerciseName,
		WorkoutExerciseExecutionId executionId,
		WorkoutOccurrenceId occurrenceId,
		LocalDate scheduledDate) {

	public PersonalRecordProvenance {
		Objects.requireNonNull(exerciseName, "exerciseName must not be null");
		Objects.requireNonNull(executionId, "executionId must not be null");
		Objects.requireNonNull(occurrenceId, "occurrenceId must not be null");
		Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
	}

}
