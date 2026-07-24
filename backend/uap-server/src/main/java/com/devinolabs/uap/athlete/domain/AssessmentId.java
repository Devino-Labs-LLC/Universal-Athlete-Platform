package com.devinolabs.uap.athlete.domain;

import java.util.Objects;
import java.util.UUID;

public final class AssessmentId {

	private final UUID value;

	private AssessmentId(UUID value) {
		this.value = Objects.requireNonNull(value, "AssessmentId value must not be null");
	}

	public static AssessmentId generate() {
		return new AssessmentId(UUID.randomUUID());
	}

	public static AssessmentId of(UUID value) {
		return new AssessmentId(value);
	}

	public static AssessmentId of(String value) {
		Objects.requireNonNull(value, "AssessmentId value must not be null");
		return new AssessmentId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AssessmentId assessmentId)) {
			return false;
		}
		return value.equals(assessmentId.value);
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
