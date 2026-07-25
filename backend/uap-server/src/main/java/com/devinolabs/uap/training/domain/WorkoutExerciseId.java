package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutExerciseId {

	private final UUID value;

	private WorkoutExerciseId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutExerciseId value must not be null");
	}

	public static WorkoutExerciseId generate() {
		return new WorkoutExerciseId(UUID.randomUUID());
	}

	public static WorkoutExerciseId of(UUID value) {
		return new WorkoutExerciseId(value);
	}

	public static WorkoutExerciseId of(String value) {
		Objects.requireNonNull(value, "WorkoutExerciseId value must not be null");
		return new WorkoutExerciseId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutExerciseId workoutExerciseId)) {
			return false;
		}
		return value.equals(workoutExerciseId.value);
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
