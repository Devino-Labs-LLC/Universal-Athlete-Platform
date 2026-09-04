package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic mean/median/min/max/sample-stddev over BigDecimal observations.
 */
public final class RecoveryMetricStatisticsCalculator {

	private static final MathContext MATH = MathContext.DECIMAL128;
	public static final int MEAN_SCALE = 2;
	public static final int MEDIAN_SCALE = 2;
	public static final int STDDEV_SCALE = 4;

	private RecoveryMetricStatisticsCalculator() {
	}

	public static RecoveryMetricStatistics calculate(List<BigDecimal> values) {
		Objects.requireNonNull(values, "values must not be null");
		if (values.isEmpty()) {
			return RecoveryMetricStatistics.empty();
		}
		List<BigDecimal> ordered = new ArrayList<>(values);
		Collections.sort(ordered);
		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal value : ordered) {
			sum = sum.add(value, MATH);
		}
		BigDecimal mean = sum.divide(BigDecimal.valueOf(ordered.size()), MEAN_SCALE, RoundingMode.HALF_UP);
		BigDecimal median = median(ordered);
		BigDecimal minimum = ordered.getFirst();
		BigDecimal maximum = ordered.getLast();
		BigDecimal stdDev = sampleStandardDeviation(ordered, mean);
		return new RecoveryMetricStatistics(ordered.size(), mean, median, minimum, maximum, stdDev);
	}

	static BigDecimal median(List<BigDecimal> ordered) {
		int size = ordered.size();
		if (size % 2 == 1) {
			return ordered.get(size / 2).setScale(MEDIAN_SCALE, RoundingMode.HALF_UP);
		}
		BigDecimal left = ordered.get(size / 2 - 1);
		BigDecimal right = ordered.get(size / 2);
		return left.add(right, MATH).divide(BigDecimal.valueOf(2), MEDIAN_SCALE, RoundingMode.HALF_UP);
	}

	/**
	 * Sample standard deviation using denominator {@code n - 1}. Null when {@code n < 2}.
	 */
	static BigDecimal sampleStandardDeviation(List<BigDecimal> ordered, BigDecimal mean) {
		if (ordered.size() < 2) {
			return null;
		}
		BigDecimal sumSquares = BigDecimal.ZERO;
		for (BigDecimal value : ordered) {
			BigDecimal delta = value.subtract(mean, MATH);
			sumSquares = sumSquares.add(delta.multiply(delta, MATH), MATH);
		}
		BigDecimal variance = sumSquares.divide(BigDecimal.valueOf(ordered.size() - 1L), MATH);
		return sqrt(variance).setScale(STDDEV_SCALE, RoundingMode.HALF_UP);
	}

	private static BigDecimal sqrt(BigDecimal value) {
		if (value.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal x = new BigDecimal(Double.toString(Math.sqrt(value.doubleValue())), MATH);
		// Newton refinement for DECIMAL128 stability on small integers/ratings.
		for (int i = 0; i < 8; i++) {
			x = x.add(value.divide(x, MATH), MATH).divide(BigDecimal.valueOf(2), MATH);
		}
		return x;
	}

	public record RecoveryMetricStatistics(
			int observationCount,
			BigDecimal mean,
			BigDecimal median,
			BigDecimal minimum,
			BigDecimal maximum,
			BigDecimal standardDeviation) {

		public static RecoveryMetricStatistics empty() {
			return new RecoveryMetricStatistics(0, null, null, null, null, null);
		}

	}

}
