package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Bounded readiness score 0.00–100.00. Training-context indicator only.
 */
public final class ReadinessScore {

	private static final BigDecimal MIN = new BigDecimal("0.00");
	private static final BigDecimal MAX = new BigDecimal("100.00");
	private static final int SCALE = 2;

	private final BigDecimal value;

	private ReadinessScore(BigDecimal value) {
		this.value = value;
	}

	public static ReadinessScore of(BigDecimal value) {
		Objects.requireNonNull(value, "value must not be null");
		BigDecimal normalized;
		try {
			normalized = value.setScale(SCALE, RoundingMode.HALF_UP);
		}
		catch (ArithmeticException ex) {
			throw new ReadinessNumericOverflowException("Readiness score overflow", ex);
		}
		if (normalized.compareTo(MIN) < 0 || normalized.compareTo(MAX) > 0) {
			throw new ReadinessNumericOverflowException(
					"Readiness score must be between 0.00 and 100.00 inclusive");
		}
		return new ReadinessScore(normalized);
	}

	public BigDecimal value() {
		return value;
	}

	public ReadinessBand band() {
		if (value.compareTo(new BigDecimal("75.00")) >= 0) {
			return ReadinessBand.HIGH;
		}
		if (value.compareTo(new BigDecimal("50.00")) >= 0) {
			return ReadinessBand.MODERATE;
		}
		return ReadinessBand.LOW;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ReadinessScore that)) {
			return false;
		}
		return value.compareTo(that.value) == 0;
	}

	@Override
	public int hashCode() {
		return value.stripTrailingZeros().hashCode();
	}

	@Override
	public String toString() {
		return value.toPlainString();
	}

}
