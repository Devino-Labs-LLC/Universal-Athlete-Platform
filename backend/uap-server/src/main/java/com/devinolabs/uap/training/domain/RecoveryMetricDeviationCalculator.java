package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Compares a target observation to a prior-only baseline using absolute and standardized deviation.
 */
public final class RecoveryMetricDeviationCalculator {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final BigDecimal HALF = new BigDecimal("0.5");
	private static final BigDecimal ONE_POINT_FIVE = new BigDecimal("1.5");

	private RecoveryMetricDeviationCalculator() {
	}

	public static RecoveryMetricDeviationResult compare(
			RecoveryMetricType metricType,
			BigDecimal targetValue,
			RecoveryMetricBaseline baseline) {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(baseline, "baseline must not be null");
		if (targetValue == null) {
			return RecoveryMetricDeviationResult.missingTarget(metricType, baseline);
		}
		if (baseline.observationCount() == 0) {
			return new RecoveryMetricDeviationResult(
					metricType,
					targetValue,
					null,
					null,
					null,
					null,
					RecoveryComparisonBand.INSUFFICIENT_DATA,
					metricType.scaleDirection(),
					baseline.dataSufficiency(),
					RecoveryAnalyticsReasonCode.NO_PRIOR_OBSERVATIONS);
		}
		if (baseline.dataSufficiency() == RecoveryBaselineDataSufficiency.INSUFFICIENT) {
			return new RecoveryMetricDeviationResult(
					metricType,
					targetValue,
					baseline.mean(),
					signedDifference(targetValue, baseline.mean()),
					null,
					null,
					RecoveryComparisonBand.INSUFFICIENT_DATA,
					metricType.scaleDirection(),
					baseline.dataSufficiency(),
					RecoveryAnalyticsReasonCode.INSUFFICIENT_PRIOR_OBSERVATIONS);
		}
		BigDecimal mean = baseline.mean();
		BigDecimal absoluteDifference = signedDifference(targetValue, mean);
		BigDecimal percentageDifference = null;
		if (!metricType.ordinalRating() && mean != null && mean.compareTo(BigDecimal.ZERO) > 0) {
			percentageDifference = targetValue.subtract(mean, MATH)
					.multiply(BigDecimal.valueOf(100), MATH)
					.divide(mean, 2, RoundingMode.HALF_UP);
		}
		BigDecimal stdDev = baseline.standardDeviation();
		BigDecimal z = null;
		RecoveryAnalyticsReasonCode reason = RecoveryAnalyticsReasonCode.BASELINE_AVAILABLE;
		if (stdDev == null) {
			reason = RecoveryAnalyticsReasonCode.INSUFFICIENT_PRIOR_OBSERVATIONS;
		}
		else if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
			if (targetValue.compareTo(mean) == 0) {
				z = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
			}
			else {
				reason = RecoveryAnalyticsReasonCode.ZERO_BASELINE_VARIANCE;
			}
		}
		else {
			z = targetValue.subtract(mean, MATH).divide(stdDev, 4, RoundingMode.HALF_UP);
		}
		RecoveryComparisonBand band = band(z, targetValue, mean, baseline.dataSufficiency());
		return new RecoveryMetricDeviationResult(
				metricType,
				targetValue,
				mean,
				absoluteDifference,
				percentageDifference,
				z,
				band,
				metricType.scaleDirection(),
				baseline.dataSufficiency(),
				reason);
	}

	private static BigDecimal signedDifference(BigDecimal target, BigDecimal mean) {
		if (mean == null) {
			return null;
		}
		return target.subtract(mean, MATH).setScale(2, RoundingMode.HALF_UP);
	}

	static RecoveryComparisonBand band(
			BigDecimal z,
			BigDecimal target,
			BigDecimal mean,
			RecoveryBaselineDataSufficiency sufficiency) {
		if (sufficiency == RecoveryBaselineDataSufficiency.INSUFFICIENT) {
			return RecoveryComparisonBand.INSUFFICIENT_DATA;
		}
		if (z != null) {
			if (z.compareTo(ONE_POINT_FIVE.negate()) <= 0) {
				return RecoveryComparisonBand.FAR_BELOW_BASELINE;
			}
			if (z.compareTo(HALF.negate()) <= 0) {
				return RecoveryComparisonBand.BELOW_BASELINE;
			}
			if (z.compareTo(HALF) < 0) {
				return RecoveryComparisonBand.WITHIN_BASELINE_RANGE;
			}
			if (z.compareTo(ONE_POINT_FIVE) < 0) {
				return RecoveryComparisonBand.ABOVE_BASELINE;
			}
			return RecoveryComparisonBand.FAR_ABOVE_BASELINE;
		}
		if (mean != null && target.compareTo(mean) == 0) {
			return RecoveryComparisonBand.WITHIN_BASELINE_RANGE;
		}
		if (sufficiency == RecoveryBaselineDataSufficiency.LIMITED && mean != null) {
			return target.compareTo(mean) < 0
					? RecoveryComparisonBand.BELOW_BASELINE
					: RecoveryComparisonBand.ABOVE_BASELINE;
		}
		return RecoveryComparisonBand.INSUFFICIENT_DATA;
	}

	public record RecoveryMetricDeviationResult(
			RecoveryMetricType metricType,
			BigDecimal targetValue,
			BigDecimal baselineMean,
			BigDecimal absoluteDifference,
			BigDecimal percentageDifference,
			BigDecimal standardizedDeviation,
			RecoveryComparisonBand comparisonBand,
			RecoveryMetricDirection scaleDirection,
			RecoveryBaselineDataSufficiency dataSufficiency,
			RecoveryAnalyticsReasonCode reasonCode) {

		public static RecoveryMetricDeviationResult missingTarget(
				RecoveryMetricType metricType,
				RecoveryMetricBaseline baseline) {
			return new RecoveryMetricDeviationResult(
					metricType,
					null,
					baseline.mean(),
					null,
					null,
					null,
					RecoveryComparisonBand.INSUFFICIENT_DATA,
					metricType.scaleDirection(),
					baseline.dataSufficiency(),
					RecoveryAnalyticsReasonCode.TARGET_VALUE_MISSING);
		}

	}

}
