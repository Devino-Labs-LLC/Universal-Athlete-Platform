package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteExercisePersonalRecordHistoryId {

	private final UUID value;

	private AthleteExercisePersonalRecordHistoryId(UUID value) {
		this.value = Objects.requireNonNull(
				value, "AthleteExercisePersonalRecordHistoryId value must not be null");
	}

	public static AthleteExercisePersonalRecordHistoryId generate() {
		return new AthleteExercisePersonalRecordHistoryId(UUID.randomUUID());
	}

	public static AthleteExercisePersonalRecordHistoryId of(UUID value) {
		return new AthleteExercisePersonalRecordHistoryId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteExercisePersonalRecordHistoryId historyId)) {
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
