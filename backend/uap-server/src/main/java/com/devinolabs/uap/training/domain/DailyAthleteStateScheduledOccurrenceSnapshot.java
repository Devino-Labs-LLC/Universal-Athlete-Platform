package com.devinolabs.uap.training.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record DailyAthleteStateScheduledOccurrenceSnapshot(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		LocalDate scheduledDate,
		WorkoutOccurrenceStatus occurrenceStatus,
		String plannedEnvironmentNameSnapshot,
		String actualEnvironmentNameSnapshot,
		int orderIndex) {

	public DailyAthleteStateScheduledOccurrenceSnapshot {
		Objects.requireNonNull(occurrenceId, "occurrenceId must not be null");
		Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		Objects.requireNonNull(occurrenceStatus, "occurrenceStatus must not be null");
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
	}

}
