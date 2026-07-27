package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * External load moved by a set, expressed in kilogram-repetitions.
 *
 * <p>Bodyweight-only work carries no external load and therefore produces no volume.
 */
public final class SetVolume implements Comparable<SetVolume> {

	private final BigDecimal kilogramRepetitions;

	private SetVolume(BigDecimal kilogramRepetitions) {
		this.kilogramRepetitions = Objects.requireNonNull(
				kilogramRepetitions, "kilogramRepetitions must not be null");
	}

	static SetVolume ofKilogramRepetitions(BigDecimal kilogramRepetitions) {
		return new SetVolume(UnitNormalizationService.toMeasurementScale(kilogramRepetitions));
	}

	public BigDecimal kilogramRepetitions() {
		return kilogramRepetitions;
	}

	public SetVolume plus(SetVolume other) {
		Objects.requireNonNull(other, "other must not be null");
		return ofKilogramRepetitions(kilogramRepetitions.add(other.kilogramRepetitions));
	}

	public boolean isPositive() {
		return kilogramRepetitions.signum() > 0;
	}

	@Override
	public int compareTo(SetVolume other) {
		return kilogramRepetitions.compareTo(other.kilogramRepetitions);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SetVolume setVolume)) {
			return false;
		}
		return kilogramRepetitions.compareTo(setVolume.kilogramRepetitions) == 0;
	}

	@Override
	public int hashCode() {
		return kilogramRepetitions.stripTrailingZeros().hashCode();
	}

	@Override
	public String toString() {
		return kilogramRepetitions.toPlainString() + " kg-reps";
	}

}
