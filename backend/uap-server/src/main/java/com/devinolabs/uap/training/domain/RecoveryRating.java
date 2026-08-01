package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Bounded integer wellness observation on a 1–5 scale.
 *
 * <p>Field-specific meaning is provided by typed wrappers ({@link FatigueRating}, etc.). This is
 * athlete-reported observation only — never a calculated physiological or medical score.
 */
public final class RecoveryRating {

	public static final int MIN = 1;
	public static final int MAX = 5;

	private final int value;

	private RecoveryRating(int value) {
		this.value = value;
	}

	public static RecoveryRating of(int value, String fieldName, RuntimeException invalidException) {
		Objects.requireNonNull(fieldName, "fieldName must not be null");
		Objects.requireNonNull(invalidException, "invalidException must not be null");
		if (value < MIN || value > MAX) {
			throw invalidException;
		}
		return new RecoveryRating(value);
	}

	public int value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RecoveryRating that)) {
			return false;
		}
		return value == that.value;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(value);
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

}
