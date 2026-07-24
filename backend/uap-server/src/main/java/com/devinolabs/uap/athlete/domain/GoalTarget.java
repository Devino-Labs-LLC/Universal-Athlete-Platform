package com.devinolabs.uap.athlete.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class GoalTarget {

	private static final int SCALE = 3;
	private static final int MAX_CUSTOM_UNIT_LENGTH = 60;

	private final BigDecimal value;
	private final GoalTargetUnit unit;
	private final String customUnit;

	private GoalTarget(BigDecimal value, GoalTargetUnit unit, String customUnit) {
		Objects.requireNonNull(value, "target value must not be null");
		Objects.requireNonNull(unit, "target unit must not be null");
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("target value must be greater than zero");
		}
		this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
		this.unit = unit;
		this.customUnit = normalizeCustomUnit(unit, customUnit);
	}

	public static GoalTarget none() {
		return null;
	}

	public static GoalTarget of(BigDecimal value, GoalTargetUnit unit) {
		return of(value, unit, null);
	}

	public static GoalTarget of(BigDecimal value, GoalTargetUnit unit, String customUnit) {
		return new GoalTarget(value, unit, customUnit);
	}

	/**
	 * Builds a target when either both value and unit are present, or both are absent.
	 *
	 * @return {@code null} when both absent ({@link #none()})
	 */
	public static GoalTarget optional(BigDecimal value, GoalTargetUnit unit, String customUnit) {
		boolean hasValue = value != null;
		boolean hasUnit = unit != null;
		if (hasValue != hasUnit) {
			throw new IllegalArgumentException("target value and unit must both be present or both be absent");
		}
		if (!hasValue) {
			if (customUnit != null && !customUnit.isBlank()) {
				throw new IllegalArgumentException("customTargetUnit must be absent when no target is set");
			}
			return none();
		}
		return of(value, unit, customUnit);
	}

	private static String normalizeCustomUnit(GoalTargetUnit unit, String customUnit) {
		if (unit == GoalTargetUnit.OTHER) {
			if (customUnit == null || customUnit.isBlank()) {
				throw new IllegalArgumentException("customTargetUnit is required when targetUnit is OTHER");
			}
			String normalized = customUnit.trim();
			if (normalized.length() > MAX_CUSTOM_UNIT_LENGTH) {
				throw new IllegalArgumentException(
						"customTargetUnit must not exceed " + MAX_CUSTOM_UNIT_LENGTH + " characters");
			}
			return normalized;
		}
		if (customUnit != null && !customUnit.isBlank()) {
			throw new IllegalArgumentException("customTargetUnit must be absent unless targetUnit is OTHER");
		}
		return null;
	}

	public BigDecimal value() {
		return value;
	}

	public GoalTargetUnit unit() {
		return unit;
	}

	public String customUnit() {
		return customUnit;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GoalTarget goalTarget)) {
			return false;
		}
		return value.compareTo(goalTarget.value) == 0
				&& unit == goalTarget.unit
				&& Objects.equals(customUnit, goalTarget.customUnit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value.stripTrailingZeros(), unit, customUnit);
	}

}
