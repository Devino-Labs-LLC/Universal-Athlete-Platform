package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record DailyAthleteStateCategorySummarySnapshot(
		ExerciseDefinitionCategory category,
		long completedExerciseCount,
		long completedSetCount,
		BigDecimal volumeKilograms,
		long durationSeconds,
		BigDecimal distanceMeters) {

	public DailyAthleteStateCategorySummarySnapshot {
		Objects.requireNonNull(category, "category must not be null");
		Objects.requireNonNull(volumeKilograms, "volumeKilograms must not be null");
		Objects.requireNonNull(distanceMeters, "distanceMeters must not be null");
	}

}
