package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record DailyAthleteStateMovementSummarySnapshot(
		MovementPattern movementPattern,
		long completedExerciseCount,
		long completedSetCount,
		long completedRepetitionCount,
		BigDecimal volumeKilograms,
		long durationSeconds,
		BigDecimal distanceMeters) {

	public DailyAthleteStateMovementSummarySnapshot {
		Objects.requireNonNull(movementPattern, "movementPattern must not be null");
		Objects.requireNonNull(volumeKilograms, "volumeKilograms must not be null");
		Objects.requireNonNull(distanceMeters, "distanceMeters must not be null");
	}

}
