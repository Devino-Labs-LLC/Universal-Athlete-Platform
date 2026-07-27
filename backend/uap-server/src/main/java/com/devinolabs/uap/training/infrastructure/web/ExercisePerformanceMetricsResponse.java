package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.ExercisePerformanceMetrics;

/**
 * @param bestEstimatedOneRepMax always flagged estimated; never confuse it with heaviestWeight,
 *                               which is a load the athlete actually moved
 */
record ExercisePerformanceMetricsResponse(
		int completedSetCount,
		Integer totalRepetitions,
		Integer mostRepetitionsInSet,
		PerformanceMeasurementResponse heaviestWeight,
		PerformanceMeasurementResponse bestEstimatedOneRepMax,
		PerformanceMeasurementResponse bestSetVolume,
		PerformanceMeasurementResponse totalVolume,
		Integer longestSetDurationSeconds,
		Integer totalDurationSeconds,
		PerformanceMeasurementResponse longestSetDistance,
		PerformanceMeasurementResponse totalDistance,
		BigDecimal averageRpe) {

	static ExercisePerformanceMetricsResponse from(ExercisePerformanceMetrics metrics) {
		return new ExercisePerformanceMetricsResponse(
				metrics.completedSetCount(),
				metrics.totalRepetitions(),
				metrics.mostRepetitionsInSet(),
				PerformanceMeasurementResponse.from(metrics.heaviestWeight()),
				PerformanceMeasurementResponse.from(metrics.bestEstimatedOneRepMax()),
				PerformanceMeasurementResponse.from(metrics.bestSetVolume()),
				PerformanceMeasurementResponse.from(metrics.totalVolume()),
				metrics.longestSetDurationSeconds(),
				metrics.totalDurationSeconds(),
				PerformanceMeasurementResponse.from(metrics.longestSetDistance()),
				PerformanceMeasurementResponse.from(metrics.totalDistance()),
				metrics.averageRpe());
	}

}
