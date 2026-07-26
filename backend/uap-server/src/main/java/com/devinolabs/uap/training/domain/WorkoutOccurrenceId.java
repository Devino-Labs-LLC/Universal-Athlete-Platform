package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutOccurrenceId {

	private final UUID value;

	private WorkoutOccurrenceId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutOccurrenceId value must not be null");
	}

	public static WorkoutOccurrenceId generate() {
		return new WorkoutOccurrenceId(UUID.randomUUID());
	}

	public static WorkoutOccurrenceId of(UUID value) {
		return new WorkoutOccurrenceId(value);
	}

	public static WorkoutOccurrenceId of(String value) {
		Objects.requireNonNull(value, "WorkoutOccurrenceId value must not be null");
		return new WorkoutOccurrenceId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutOccurrenceId workoutOccurrenceId)) {
			return false;
		}
		return value.equals(workoutOccurrenceId.value);
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
