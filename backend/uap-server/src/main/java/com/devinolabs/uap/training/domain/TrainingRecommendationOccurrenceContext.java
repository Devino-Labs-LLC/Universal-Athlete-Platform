package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Factual scheduled-occurrence context copied from the immutable athlete-state snapshot.
 */
public record TrainingRecommendationOccurrenceContext(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		WorkoutOccurrenceStatus occurrenceStatus,
		boolean modifiable,
		String plannedEnvironmentNameSnapshot,
		String actualEnvironmentNameSnapshot,
		int orderIndex) {

	public TrainingRecommendationOccurrenceContext {
		Objects.requireNonNull(occurrenceId, "occurrenceId must not be null");
		Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		Objects.requireNonNull(occurrenceStatus, "occurrenceStatus must not be null");
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
	}

	public static boolean isModifiable(WorkoutOccurrenceStatus status) {
		return status == WorkoutOccurrenceStatus.SCHEDULED
				|| status == WorkoutOccurrenceStatus.IN_PROGRESS;
	}

}
