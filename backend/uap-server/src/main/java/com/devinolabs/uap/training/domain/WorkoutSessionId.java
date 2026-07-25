package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutSessionId {

	private final UUID value;

	private WorkoutSessionId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutSessionId value must not be null");
	}

	public static WorkoutSessionId generate() {
		return new WorkoutSessionId(UUID.randomUUID());
	}

	public static WorkoutSessionId of(UUID value) {
		return new WorkoutSessionId(value);
	}

	public static WorkoutSessionId of(String value) {
		Objects.requireNonNull(value, "WorkoutSessionId value must not be null");
		return new WorkoutSessionId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutSessionId workoutSessionId)) {
			return false;
		}
		return value.equals(workoutSessionId.value);
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
