package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class ExerciseDefinitionId {

	private final UUID value;

	private ExerciseDefinitionId(UUID value) {
		this.value = Objects.requireNonNull(value, "ExerciseDefinitionId value must not be null");
	}

	public static ExerciseDefinitionId generate() {
		return new ExerciseDefinitionId(UUID.randomUUID());
	}

	public static ExerciseDefinitionId of(UUID value) {
		return new ExerciseDefinitionId(value);
	}

	public static ExerciseDefinitionId of(String value) {
		Objects.requireNonNull(value, "ExerciseDefinitionId value must not be null");
		return new ExerciseDefinitionId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ExerciseDefinitionId exerciseDefinitionId)) {
			return false;
		}
		return value.equals(exerciseDefinitionId.value);
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
