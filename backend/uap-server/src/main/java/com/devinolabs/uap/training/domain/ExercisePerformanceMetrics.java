package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;

/**
 * Performance figures derived from the completed sets of one exercise execution.
 *
 * <p>Every nullable field is absent because the underlying sets carried no usable measurement; for
 * example bodyweight-only work has no volume and no heaviest weight.
 */
public record ExercisePerformanceMetrics(
		int completedSetCount,
		Integer totalRepetitions,
		Integer mostRepetitionsInSet,
		PerformanceMeasurement heaviestWeight,
		PerformanceMeasurement bestEstimatedOneRepMax,
		PerformanceMeasurement bestSetVolume,
		PerformanceMeasurement totalVolume,
		Integer longestSetDurationSeconds,
		Integer totalDurationSeconds,
		PerformanceMeasurement longestSetDistance,
		PerformanceMeasurement totalDistance,
		BigDecimal averageRpe) {

	public static ExercisePerformanceMetrics empty() {
		return new ExercisePerformanceMetrics(
				0, null, null, null, null, null, null, null, null, null, null, null);
	}

}
