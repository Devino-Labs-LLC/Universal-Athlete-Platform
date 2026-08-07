package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class DailyReadinessAssessmentId {

	private final UUID value;

	private DailyReadinessAssessmentId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static DailyReadinessAssessmentId of(UUID value) {
		return new DailyReadinessAssessmentId(value);
	}

	public static DailyReadinessAssessmentId of(String value) {
		return of(UUID.fromString(value));
	}

	public static DailyReadinessAssessmentId generate() {
		return of(UUID.randomUUID());
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof DailyReadinessAssessmentId that)) {
			return false;
		}
		return value.equals(that.value);
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
