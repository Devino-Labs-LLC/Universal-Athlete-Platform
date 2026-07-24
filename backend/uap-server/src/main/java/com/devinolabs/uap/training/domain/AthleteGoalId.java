package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteGoalId {

	private final UUID value;

	private AthleteGoalId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteGoalId value must not be null");
	}

	public static AthleteGoalId of(UUID value) {
		return new AthleteGoalId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteGoalId athleteGoalId)) {
			return false;
		}
		return value.equals(athleteGoalId.value);
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
