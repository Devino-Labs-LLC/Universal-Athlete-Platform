package com.devinolabs.uap.athlete.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Weight {

	private static final BigDecimal MAX_KG = new BigDecimal("500");
	private static final BigDecimal KG_PER_POUND = new BigDecimal("0.45359237");
	private static final int SCALE = 2;

	private final BigDecimal kilograms;

	private Weight(BigDecimal kilograms) {
		Objects.requireNonNull(kilograms, "Weight kilograms must not be null");
		if (kilograms.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Weight must be greater than zero");
		}
		if (kilograms.compareTo(MAX_KG) > 0) {
			throw new IllegalArgumentException("Weight must not exceed 500 kg");
		}
		this.kilograms = kilograms.setScale(SCALE, RoundingMode.HALF_UP);
	}

	public static Weight ofKilograms(BigDecimal kilograms) {
		return new Weight(kilograms);
	}

	public static Weight ofKilograms(double kilograms) {
		return ofKilograms(BigDecimal.valueOf(kilograms));
	}

	public static Weight ofPounds(BigDecimal pounds) {
		Objects.requireNonNull(pounds, "Weight pounds must not be null");
		if (pounds.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Weight must be greater than zero");
		}
		return ofKilograms(pounds.multiply(KG_PER_POUND));
	}

	public static Weight ofPounds(double pounds) {
		return ofPounds(BigDecimal.valueOf(pounds));
	}

	public BigDecimal kilograms() {
		return kilograms;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Weight weight)) {
			return false;
		}
		return kilograms.compareTo(weight.kilograms) == 0;
	}

	@Override
	public int hashCode() {
		return kilograms.stripTrailingZeros().hashCode();
	}

	@Override
	public String toString() {
		return kilograms.toPlainString() + " kg";
	}

}
