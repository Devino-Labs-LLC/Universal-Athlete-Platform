package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class WorkoutLoadMovementPatternSummary {

	private final MovementPattern primaryMovementPattern;
	private final long completedExerciseCount;
	private final long completedSetCount;
	private final long completedRepetitionCount;
	private final BigDecimal volumeKilograms;
	private final long durationSeconds;
	private final BigDecimal distanceMeters;

	public WorkoutLoadMovementPatternSummary(
			MovementPattern primaryMovementPattern,
			long completedExerciseCount,
			long completedSetCount,
			long completedRepetitionCount,
			BigDecimal volumeKilograms,
			long durationSeconds,
			BigDecimal distanceMeters) {
		this.primaryMovementPattern = Objects.requireNonNull(
				primaryMovementPattern, "primaryMovementPattern must not be null");
		this.completedExerciseCount = completedExerciseCount;
		this.completedSetCount = completedSetCount;
		this.completedRepetitionCount = completedRepetitionCount;
		this.volumeKilograms = volumeKilograms == null
				? BigDecimal.ZERO.setScale(3, java.math.RoundingMode.UNNECESSARY)
				: volumeKilograms;
		this.durationSeconds = durationSeconds;
		this.distanceMeters = distanceMeters == null
				? BigDecimal.ZERO.setScale(3, java.math.RoundingMode.UNNECESSARY)
				: distanceMeters;
	}

	public MovementPattern primaryMovementPattern() {
		return primaryMovementPattern;
	}

	public long completedExerciseCount() {
		return completedExerciseCount;
	}

	public long completedSetCount() {
		return completedSetCount;
	}

	public long completedRepetitionCount() {
		return completedRepetitionCount;
	}

	public BigDecimal volumeKilograms() {
		return volumeKilograms;
	}

	public long durationSeconds() {
		return durationSeconds;
	}

	public BigDecimal distanceMeters() {
		return distanceMeters;
	}

}
