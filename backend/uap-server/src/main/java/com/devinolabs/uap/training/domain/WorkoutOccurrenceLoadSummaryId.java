package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutOccurrenceLoadSummaryId {

	private final UUID value;

	private WorkoutOccurrenceLoadSummaryId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutOccurrenceLoadSummaryId value must not be null");
	}

	public static WorkoutOccurrenceLoadSummaryId generate() {
		return new WorkoutOccurrenceLoadSummaryId(UUID.randomUUID());
	}

	public static WorkoutOccurrenceLoadSummaryId of(UUID value) {
		return new WorkoutOccurrenceLoadSummaryId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutOccurrenceLoadSummaryId that)) {
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
