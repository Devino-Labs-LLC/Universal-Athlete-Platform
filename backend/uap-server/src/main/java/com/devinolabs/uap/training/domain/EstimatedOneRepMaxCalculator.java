package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Epley estimated one-rep max: {@code weight * (1 + reps / 30)}.
 *
 * <p>The estimate is only meaningful for loaded low-rep work, so it is produced for 1 to
 * {@value #MAX_ESTIMABLE_REPETITIONS} repetitions with a positive external weight. A single
 * repetition is a measured maximum and is returned unchanged.
 */
public final class EstimatedOneRepMaxCalculator {

	public static final int MAX_ESTIMABLE_REPETITIONS = 12;

	private static final BigDecimal EPLEY_DIVISOR = new BigDecimal("30");

	private static final MathContext CONTEXT = MathContext.DECIMAL128;

	private EstimatedOneRepMaxCalculator() {
	}

	/**
	 * @return the estimate in canonical kilograms, or {@code null} when the set is not estimable
	 */
	public static NormalizedWeight estimate(BigDecimal weight, WeightUnit unit, Integer repetitions) {
		if (weight == null || unit == null || repetitions == null) {
			return null;
		}
		if (repetitions < 1 || repetitions > MAX_ESTIMABLE_REPETITIONS) {
			return null;
		}
		NormalizedWeight normalized = UnitNormalizationService.normalizeWeight(weight, unit);
		if (!normalized.isPositive()) {
			return null;
		}
		if (repetitions == 1) {
			return normalized;
		}
		BigDecimal multiplier = BigDecimal.ONE.add(
				BigDecimal.valueOf(repetitions).divide(EPLEY_DIVISOR, CONTEXT));
		return NormalizedWeight.ofKilograms(normalized.kilograms().multiply(multiplier, CONTEXT));
	}

}
