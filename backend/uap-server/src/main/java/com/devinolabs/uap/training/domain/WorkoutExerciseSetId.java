package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutExerciseSetId {

	private final UUID value;

	private WorkoutExerciseSetId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutExerciseSetId value must not be null");
	}

	public static WorkoutExerciseSetId generate() {
		return new WorkoutExerciseSetId(UUID.randomUUID());
	}

	public static WorkoutExerciseSetId of(UUID value) {
		return new WorkoutExerciseSetId(value);
	}

	public static WorkoutExerciseSetId of(String value) {
		Objects.requireNonNull(value, "WorkoutExerciseSetId value must not be null");
		return new WorkoutExerciseSetId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutExerciseSetId workoutExerciseSetId)) {
			return false;
		}
		return value.equals(workoutExerciseSetId.value);
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
