package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Session-RPE training load in arbitrary units: {@code sessionRpe × sessionDurationMinutes}.
 *
 * <p>Null when either RPE or duration is absent. Zero RPE produces zero load; missing values are
 * never treated as zero.
 */
public final class SessionRpeLoad {

	private final BigDecimal value;

	private SessionRpeLoad(BigDecimal value) {
		this.value = value;
	}

	public static SessionRpeLoad of(SessionRpe sessionRpe, Integer sessionDurationMinutes) {
		Objects.requireNonNull(sessionRpe, "sessionRpe must not be null");
		Objects.requireNonNull(sessionDurationMinutes, "sessionDurationMinutes must not be null");
		if (sessionDurationMinutes < 1 || sessionDurationMinutes > 1440) {
			throw new InvalidSessionDurationException(
					"sessionDurationMinutes must be between 1 and 1440 inclusive");
		}
		try {
			BigDecimal load = sessionRpe.value()
					.multiply(BigDecimal.valueOf(sessionDurationMinutes))
					.setScale(2, RoundingMode.HALF_UP);
			return new SessionRpeLoad(load);
		}
		catch (ArithmeticException ex) {
			throw new TrainingLoadNumericOverflowException("Session RPE load overflowed");
		}
	}

	public static SessionRpeLoad ofNullable(SessionRpe sessionRpe, Integer sessionDurationMinutes) {
		if (sessionRpe == null || sessionDurationMinutes == null) {
			return null;
		}
		return of(sessionRpe, sessionDurationMinutes);
	}

	public static SessionRpeLoad fromPersistence(BigDecimal value) {
		Objects.requireNonNull(value, "value must not be null");
		return new SessionRpeLoad(value);
	}

	public BigDecimal value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SessionRpeLoad that)) {
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
