package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutDayId {

	private final UUID value;

	private WorkoutDayId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutDayId value must not be null");
	}

	public static WorkoutDayId generate() {
		return new WorkoutDayId(UUID.randomUUID());
	}

	public static WorkoutDayId of(UUID value) {
		return new WorkoutDayId(value);
	}

	public static WorkoutDayId of(String value) {
		Objects.requireNonNull(value, "WorkoutDayId value must not be null");
		return new WorkoutDayId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutDayId workoutDayId)) {
			return false;
		}
		return value.equals(workoutDayId.value);
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
