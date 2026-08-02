package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic trend direction by comparing earlier vs later observed-value means.
 *
 * <p>Does not describe improvement or deterioration — only INCREASING / DECREASING / STABLE.
 */
public final class RecoveryTrendCalculator {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final BigDecimal RATING_THRESHOLD = new BigDecimal("0.25");
	private static final BigDecimal SLEEP_THRESHOLD = new BigDecimal("15");

	private RecoveryTrendCalculator() {
	}

	public static RecoveryTrendDirection calculate(
			RecoveryMetricType metricType,
			List<BigDecimal> chronologicalValues) {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(chronologicalValues, "chronologicalValues must not be null");
		if (chronologicalValues.size() < 4) {
			return RecoveryTrendDirection.INSUFFICIENT_DATA;
		}
		int midpoint = chronologicalValues.size() / 2;
		List<BigDecimal> earlier = chronologicalValues.subList(0, midpoint);
		List<BigDecimal> later = chronologicalValues.subList(midpoint, chronologicalValues.size());
		if (earlier.size() < 2 || later.size() < 2) {
			return RecoveryTrendDirection.INSUFFICIENT_DATA;
		}
		BigDecimal earlierMean = mean(earlier);
		BigDecimal laterMean = mean(later);
		BigDecimal delta = laterMean.subtract(earlierMean, MATH);
		BigDecimal threshold = metricType == RecoveryMetricType.SLEEP_DURATION
				? SLEEP_THRESHOLD
				: RATING_THRESHOLD;
		if (delta.compareTo(threshold) > 0) {
			return RecoveryTrendDirection.INCREASING;
		}
		if (delta.compareTo(threshold.negate()) < 0) {
			return RecoveryTrendDirection.DECREASING;
		}
		return RecoveryTrendDirection.STABLE;
	}

	private static BigDecimal mean(List<BigDecimal> values) {
		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			sum = sum.add(value, MATH);
		}
		return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
	}

}
