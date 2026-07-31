package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class WorkoutLoadCategorySummary {

	private final ExerciseDefinitionCategory category;
	private final long completedExerciseCount;
	private final long completedSetCount;
	private final BigDecimal volumeKilograms;
	private final long durationSeconds;
	private final BigDecimal distanceMeters;

	public WorkoutLoadCategorySummary(
			ExerciseDefinitionCategory category,
			long completedExerciseCount,
			long completedSetCount,
			BigDecimal volumeKilograms,
			long durationSeconds,
			BigDecimal distanceMeters) {
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.completedExerciseCount = completedExerciseCount;
		this.completedSetCount = completedSetCount;
		this.volumeKilograms = zeroVolume(volumeKilograms);
		this.durationSeconds = durationSeconds;
		this.distanceMeters = zeroDistance(distanceMeters);
	}

	private static BigDecimal zeroVolume(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(3, java.math.RoundingMode.UNNECESSARY) : value;
	}

	private static BigDecimal zeroDistance(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(3, java.math.RoundingMode.UNNECESSARY) : value;
	}

	public ExerciseDefinitionCategory category() {
		return category;
	}

	public long completedExerciseCount() {
		return completedExerciseCount;
	}

	public long completedSetCount() {
		return completedSetCount;
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
