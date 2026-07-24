package com.devinolabs.uap.athlete.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Height {

	private static final BigDecimal MIN_CM = new BigDecimal("40");
	private static final BigDecimal MAX_CM = new BigDecimal("300");
	private static final BigDecimal CM_PER_INCH = new BigDecimal("2.54");
	private static final int SCALE = 2;

	private final BigDecimal centimeters;

	private Height(BigDecimal centimeters) {
		Objects.requireNonNull(centimeters, "Height centimeters must not be null");
		if (centimeters.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Height must be greater than zero");
		}
		if (centimeters.compareTo(MIN_CM) < 0 || centimeters.compareTo(MAX_CM) > 0) {
			throw new IllegalArgumentException("Height must be between 40 cm and 300 cm");
		}
		this.centimeters = centimeters.setScale(SCALE, RoundingMode.HALF_UP);
	}

	public static Height ofCentimeters(BigDecimal centimeters) {
		return new Height(centimeters);
	}

	public static Height ofCentimeters(double centimeters) {
		return ofCentimeters(BigDecimal.valueOf(centimeters));
	}

	public static Height ofFeetInches(int feet, int inches) {
		if (feet < 0 || inches < 0) {
			throw new IllegalArgumentException("Feet and inches must not be negative");
		}
		if (inches >= 12) {
			throw new IllegalArgumentException("Inches must be less than 12");
		}
		BigDecimal totalInches = BigDecimal.valueOf(feet)
				.multiply(BigDecimal.valueOf(12))
				.add(BigDecimal.valueOf(inches));
		return ofCentimeters(totalInches.multiply(CM_PER_INCH));
	}

	public BigDecimal centimeters() {
		return centimeters;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Height height)) {
			return false;
		}
		return centimeters.compareTo(height.centimeters) == 0;
	}

	@Override
	public int hashCode() {
		return centimeters.stripTrailingZeros().hashCode();
	}

	@Override
	public String toString() {
		return centimeters.toPlainString() + " cm";
	}

}
