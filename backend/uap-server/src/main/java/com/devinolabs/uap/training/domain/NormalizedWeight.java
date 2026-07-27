package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A weight expressed in the canonical kilogram scale used for cross-unit comparison.
 */
public final class NormalizedWeight implements Comparable<NormalizedWeight> {

	private final BigDecimal kilograms;

	private NormalizedWeight(BigDecimal kilograms) {
		this.kilograms = Objects.requireNonNull(kilograms, "kilograms must not be null");
	}

	static NormalizedWeight ofKilograms(BigDecimal kilograms) {
		return new NormalizedWeight(UnitNormalizationService.toMeasurementScale(kilograms));
	}

	public BigDecimal kilograms() {
		return kilograms;
	}

	public boolean isPositive() {
		return kilograms.signum() > 0;
	}

	@Override
	public int compareTo(NormalizedWeight other) {
		return kilograms.compareTo(other.kilograms);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NormalizedWeight normalizedWeight)) {
			return false;
		}
		return kilograms.compareTo(normalizedWeight.kilograms) == 0;
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
