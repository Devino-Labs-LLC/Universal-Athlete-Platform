package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * READINESS_V1 dimension normalization rules.
 * Heuristic training-context points — not clinical thresholds.
 */
public final class ReadinessDimensionNormalizer {

	private static final BigDecimal SCORE_100 = new BigDecimal("100.00");
	private static final BigDecimal SCORE_85 = new BigDecimal("85.00");
	private static final BigDecimal SCORE_80 = new BigDecimal("80.00");
	private static final BigDecimal SCORE_70 = new BigDecimal("70.00");
	private static final BigDecimal SCORE_65 = new BigDecimal("65.00");
	private static final BigDecimal SCORE_45 = new BigDecimal("45.00");
	private static final BigDecimal SCORE_20 = new BigDecimal("20.00");

	private static final BigDecimal Z_FAR = new BigDecimal("1.5");
	private static final BigDecimal Z_NEAR = new BigDecimal("0.5");

	private ReadinessDimensionNormalizer() {
	}

	public static Optional<NormalizedDimension> normalize(
			ReadinessDimensionType dimensionType,
			DailyAthleteStateRecoveryMetricSnapshot metric) {
		Objects.requireNonNull(dimensionType, "dimensionType must not be null");
		Objects.requireNonNull(metric, "metric must not be null");

		if (metric.targetValue() == null) {
			return Optional.empty();
		}
		if (metric.dataSufficiency() == RecoveryBaselineDataSufficiency.INSUFFICIENT
				|| metric.comparisonBand() == RecoveryComparisonBand.INSUFFICIENT_DATA) {
			return Optional.empty();
		}
		if (metric.baselineMean() == null) {
			return Optional.empty();
		}

		if (dimensionType == ReadinessDimensionType.SLEEP_DURATION) {
			return Optional.of(normalizeSleepDuration(metric));
		}

		boolean burden = isBurden(dimensionType);
		boolean usedZeroVarianceFallback = metric.standardizedDeviation() == null
				&& metric.comparisonBand() != RecoveryComparisonBand.INSUFFICIENT_DATA;

		BigDecimal normalized;
		if (metric.standardizedDeviation() != null) {
			normalized = fromZScore(metric.standardizedDeviation(), burden);
		}
		else {
			normalized = fromComparisonBand(metric.comparisonBand(), burden);
		}

		ReadinessReasonCode reason = reasonFromBand(metric.comparisonBand(), usedZeroVarianceFallback);
		return Optional.of(new NormalizedDimension(normalized, reason, usedZeroVarianceFallback));
	}

	private static NormalizedDimension normalizeSleepDuration(DailyAthleteStateRecoveryMetricSnapshot metric) {
		BigDecimal score = switch (metric.comparisonBand()) {
			case WITHIN_BASELINE_RANGE -> SCORE_80;
			case ABOVE_BASELINE, BELOW_BASELINE -> SCORE_65;
			case FAR_ABOVE_BASELINE, FAR_BELOW_BASELINE -> SCORE_45;
			case INSUFFICIENT_DATA -> throw new IllegalStateException("insufficient sleep duration");
		};
		boolean zeroVarianceFallback = metric.standardizedDeviation() == null;
		return new NormalizedDimension(
				score,
				reasonFromBand(metric.comparisonBand(), zeroVarianceFallback),
				zeroVarianceFallback);
	}

	static BigDecimal fromZScore(BigDecimal z, boolean burden) {
		BigDecimal absFar = Z_FAR;
		BigDecimal absNear = Z_NEAR;
		if (burden) {
			if (z.compareTo(absFar.negate()) <= 0) {
				return SCORE_100;
			}
			if (z.compareTo(absNear.negate()) <= 0) {
				return SCORE_85;
			}
			if (z.compareTo(absNear) < 0) {
				return SCORE_70;
			}
			if (z.compareTo(absFar) < 0) {
				return SCORE_45;
			}
			return SCORE_20;
		}
		if (z.compareTo(absFar) >= 0) {
			return SCORE_100;
		}
		if (z.compareTo(absNear) >= 0) {
			return SCORE_85;
		}
		if (z.compareTo(absNear.negate()) > 0) {
			return SCORE_70;
		}
		if (z.compareTo(absFar.negate()) > 0) {
			return SCORE_45;
		}
		return SCORE_20;
	}

	static BigDecimal fromComparisonBand(RecoveryComparisonBand band, boolean burden) {
		if (burden) {
			return switch (band) {
				case FAR_BELOW_BASELINE -> SCORE_100;
				case BELOW_BASELINE -> SCORE_85;
				case WITHIN_BASELINE_RANGE -> SCORE_70;
				case ABOVE_BASELINE -> SCORE_45;
				case FAR_ABOVE_BASELINE -> SCORE_20;
				case INSUFFICIENT_DATA -> SCORE_70;
			};
		}
		return switch (band) {
			case FAR_ABOVE_BASELINE -> SCORE_100;
			case ABOVE_BASELINE -> SCORE_85;
			case WITHIN_BASELINE_RANGE -> SCORE_70;
			case BELOW_BASELINE -> SCORE_45;
			case FAR_BELOW_BASELINE -> SCORE_20;
			case INSUFFICIENT_DATA -> SCORE_70;
		};
	}

	private static ReadinessReasonCode reasonFromBand(
			RecoveryComparisonBand band,
			boolean zeroVarianceFallback) {
		if (zeroVarianceFallback && band != RecoveryComparisonBand.WITHIN_BASELINE_RANGE) {
			// Prefer explicit zero-variance reason when z unavailable and not simply aligned.
			if (band == RecoveryComparisonBand.ABOVE_BASELINE
					|| band == RecoveryComparisonBand.BELOW_BASELINE
					|| band == RecoveryComparisonBand.FAR_ABOVE_BASELINE
					|| band == RecoveryComparisonBand.FAR_BELOW_BASELINE) {
				return ReadinessReasonCode.ZERO_BASELINE_VARIANCE_FALLBACK;
			}
		}
		return switch (band) {
			case WITHIN_BASELINE_RANGE -> ReadinessReasonCode.BASELINE_ALIGNED;
			case ABOVE_BASELINE -> ReadinessReasonCode.ABOVE_PERSONAL_BASELINE;
			case BELOW_BASELINE -> ReadinessReasonCode.BELOW_PERSONAL_BASELINE;
			case FAR_ABOVE_BASELINE -> ReadinessReasonCode.FAR_ABOVE_PERSONAL_BASELINE;
			case FAR_BELOW_BASELINE -> ReadinessReasonCode.FAR_BELOW_PERSONAL_BASELINE;
			case INSUFFICIENT_DATA -> ReadinessReasonCode.BASELINE_INSUFFICIENT;
		};
	}

	private static boolean isBurden(ReadinessDimensionType dimensionType) {
		return dimensionType == ReadinessDimensionType.FATIGUE
				|| dimensionType == ReadinessDimensionType.MUSCLE_SORENESS
				|| dimensionType == ReadinessDimensionType.STRESS;
	}

	public record NormalizedDimension(
			BigDecimal normalizedScore,
			ReadinessReasonCode reasonCode,
			boolean zeroVarianceFallback) {

		public NormalizedDimension {
			normalizedScore = normalizedScore.setScale(2, RoundingMode.HALF_UP);
			Objects.requireNonNull(reasonCode, "reasonCode must not be null");
		}
	}

}
