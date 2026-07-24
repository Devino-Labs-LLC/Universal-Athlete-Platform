package com.devinolabs.uap.athlete.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteMeasurementId {

	private final UUID value;

	private AthleteMeasurementId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteMeasurementId value must not be null");
	}

	public static AthleteMeasurementId generate() {
		return new AthleteMeasurementId(UUID.randomUUID());
	}

	public static AthleteMeasurementId of(UUID value) {
		return new AthleteMeasurementId(value);
	}

	public static AthleteMeasurementId of(String value) {
		Objects.requireNonNull(value, "AthleteMeasurementId value must not be null");
		return new AthleteMeasurementId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteMeasurementId measurementId)) {
			return false;
		}
		return value.equals(measurementId.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
