package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteId {

	private final UUID value;

	private AthleteId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteId value must not be null");
	}

	public static AthleteId of(UUID value) {
		return new AthleteId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteId athleteId)) {
			return false;
		}
		return value.equals(athleteId.value);
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
