package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteExercisePersonalRecordId {

	private final UUID value;

	private AthleteExercisePersonalRecordId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteExercisePersonalRecordId value must not be null");
	}

	public static AthleteExercisePersonalRecordId generate() {
		return new AthleteExercisePersonalRecordId(UUID.randomUUID());
	}

	public static AthleteExercisePersonalRecordId of(UUID value) {
		return new AthleteExercisePersonalRecordId(value);
	}

	public static AthleteExercisePersonalRecordId of(String value) {
		Objects.requireNonNull(value, "AthleteExercisePersonalRecordId value must not be null");
		return new AthleteExercisePersonalRecordId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteExercisePersonalRecordId personalRecordId)) {
			return false;
		}
		return value.equals(personalRecordId.value);
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
