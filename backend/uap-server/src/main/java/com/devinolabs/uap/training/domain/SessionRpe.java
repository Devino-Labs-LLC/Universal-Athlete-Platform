package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Athlete-reported session rating of perceived exertion on a 0.0–10.0 scale (one decimal place).
 *
 * <p>This value is factual athlete input only. It is never medical, psychological, or readiness
 * interpretation, and the system never invents it.
 */
public final class SessionRpe {

	private static final BigDecimal MIN = new BigDecimal("0.0");
	private static final BigDecimal MAX = new BigDecimal("10.0");

	private final BigDecimal value;

	private SessionRpe(BigDecimal value) {
		this.value = value;
	}

	public static SessionRpe of(BigDecimal value) {
		Objects.requireNonNull(value, "sessionRpe must not be null");
		final BigDecimal normalized;
		try {
			normalized = value.setScale(1, RoundingMode.UNNECESSARY);
		}
		catch (ArithmeticException ex) {
			throw new InvalidSessionRpeException("sessionRpe supports at most one decimal place");
		}
		if (normalized.compareTo(MIN) < 0 || normalized.compareTo(MAX) > 0) {
			throw new InvalidSessionRpeException("sessionRpe must be between 0.0 and 10.0 inclusive");
		}
		return new SessionRpe(normalized);
	}

	public static SessionRpe of(double value) {
		return of(BigDecimal.valueOf(value));
	}

	public BigDecimal value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SessionRpe that)) {
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
