package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Observation-based rolling averages. Missing calendar dates are never zero-filled.
 */
public final class RecoveryRollingAverageCalculator {

	private static final MathContext MATH = MathContext.DECIMAL128;

	private RecoveryRollingAverageCalculator() {
	}

	public static List<BigDecimal> rollingAverages(List<BigDecimal> chronologicalValues, int window) {
		Objects.requireNonNull(chronologicalValues, "chronologicalValues must not be null");
		if (window < 1) {
			throw new IllegalArgumentException("window must be at least 1");
		}
		List<BigDecimal> result = new ArrayList<>(chronologicalValues.size());
		for (int i = 0; i < chronologicalValues.size(); i++) {
			if (i + 1 < window) {
				result.add(null);
				continue;
			}
			BigDecimal sum = BigDecimal.ZERO;
			for (int j = i - window + 1; j <= i; j++) {
				sum = sum.add(chronologicalValues.get(j), MATH);
			}
			result.add(sum.divide(BigDecimal.valueOf(window), 2, RoundingMode.HALF_UP));
		}
		return result;
	}

}
