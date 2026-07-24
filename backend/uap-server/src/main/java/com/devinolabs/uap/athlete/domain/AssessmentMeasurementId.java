package com.devinolabs.uap.athlete.domain;

import java.util.Objects;
import java.util.UUID;

public final class AssessmentMeasurementId {

	private final UUID value;

	private AssessmentMeasurementId(UUID value) {
		this.value = Objects.requireNonNull(value, "AssessmentMeasurementId value must not be null");
	}

	public static AssessmentMeasurementId generate() {
		return new AssessmentMeasurementId(UUID.randomUUID());
	}

	public static AssessmentMeasurementId of(UUID value) {
		return new AssessmentMeasurementId(value);
	}

	public static AssessmentMeasurementId of(String value) {
		Objects.requireNonNull(value, "AssessmentMeasurementId value must not be null");
		return new AssessmentMeasurementId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AssessmentMeasurementId assessmentMeasurementId)) {
			return false;
		}
		return value.equals(assessmentMeasurementId.value);
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
