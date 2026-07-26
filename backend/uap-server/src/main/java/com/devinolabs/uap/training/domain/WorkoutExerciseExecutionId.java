package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutExerciseExecutionId {

	private final UUID value;

	private WorkoutExerciseExecutionId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutExerciseExecutionId value must not be null");
	}

	public static WorkoutExerciseExecutionId generate() {
		return new WorkoutExerciseExecutionId(UUID.randomUUID());
	}

	public static WorkoutExerciseExecutionId of(UUID value) {
		return new WorkoutExerciseExecutionId(value);
	}

	public static WorkoutExerciseExecutionId of(String value) {
		Objects.requireNonNull(value, "WorkoutExerciseExecutionId value must not be null");
		return new WorkoutExerciseExecutionId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutExerciseExecutionId workoutExerciseExecutionId)) {
			return false;
		}
		return value.equals(workoutExerciseExecutionId.value);
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
