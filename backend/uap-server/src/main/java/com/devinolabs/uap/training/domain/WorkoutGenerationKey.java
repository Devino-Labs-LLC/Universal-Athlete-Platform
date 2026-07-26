package com.devinolabs.uap.training.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Deterministic identity of a generated placement: {@code planId|dayId|scheduledDate|cycle}.
 *
 * <p>The key is stable across regenerations, which makes generation idempotent and lets a cancelled
 * occurrence act as a tombstone that suppresses recreation of the same placement.
 */
public record WorkoutGenerationKey(String value) {

	public static final int MAX_LENGTH = 200;

	public WorkoutGenerationKey {
		Objects.requireNonNull(value, "value must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException("generationKey must not be blank");
		}
		if (value.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("generationKey must not exceed " + MAX_LENGTH + " characters");
		}
	}

	public static WorkoutGenerationKey of(
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			LocalDate scheduledDate,
			int planCycleNumber) {
		Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		if (planCycleNumber < 1) {
			throw new IllegalArgumentException("planCycleNumber must be at least 1");
		}
		return new WorkoutGenerationKey("%s|%s|%s|%d".formatted(
				trainingPlanId.value(),
				workoutDayId.value(),
				scheduledDate,
				planCycleNumber));
	}

	public static WorkoutGenerationKey ofNullable(String value) {
		return value == null ? null : new WorkoutGenerationKey(value);
	}

}
