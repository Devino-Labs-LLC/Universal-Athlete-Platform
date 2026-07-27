package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A distance expressed in the canonical metre scale used for cross-unit comparison.
 */
public final class NormalizedDistance implements Comparable<NormalizedDistance> {

	private final BigDecimal meters;

	private NormalizedDistance(BigDecimal meters) {
		this.meters = Objects.requireNonNull(meters, "meters must not be null");
	}

	static NormalizedDistance ofMeters(BigDecimal meters) {
		return new NormalizedDistance(UnitNormalizationService.toMeasurementScale(meters));
	}

	public BigDecimal meters() {
		return meters;
	}

	public boolean isPositive() {
		return meters.signum() > 0;
	}

	@Override
	public int compareTo(NormalizedDistance other) {
		return meters.compareTo(other.meters);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NormalizedDistance normalizedDistance)) {
			return false;
		}
		return meters.compareTo(normalizedDistance.meters) == 0;
	}

	@Override
	public int hashCode() {
		return meters.stripTrailingZeros().hashCode();
	}

	@Override
	public String toString() {
		return meters.toPlainString() + " m";
	}

}
