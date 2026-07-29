package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutExerciseSubstitutionHistoryId {

	private final UUID value;

	private WorkoutExerciseSubstitutionHistoryId(UUID value) {
		this.value = Objects.requireNonNull(
				value, "WorkoutExerciseSubstitutionHistoryId value must not be null");
	}

	public static WorkoutExerciseSubstitutionHistoryId generate() {
		return new WorkoutExerciseSubstitutionHistoryId(UUID.randomUUID());
	}

	public static WorkoutExerciseSubstitutionHistoryId of(UUID value) {
		return new WorkoutExerciseSubstitutionHistoryId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutExerciseSubstitutionHistoryId historyId)) {
			return false;
		}
		return value.equals(historyId.value);
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
