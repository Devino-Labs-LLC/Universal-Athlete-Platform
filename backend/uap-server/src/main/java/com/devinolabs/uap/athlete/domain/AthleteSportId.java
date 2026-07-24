package com.devinolabs.uap.athlete.domain;

import java.util.Objects;
import java.util.UUID;

public final class AthleteSportId {

	private final UUID value;

	private AthleteSportId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteSportId value must not be null");
	}

	public static AthleteSportId generate() {
		return new AthleteSportId(UUID.randomUUID());
	}

	public static AthleteSportId of(UUID value) {
		return new AthleteSportId(value);
	}

	public static AthleteSportId of(String value) {
		Objects.requireNonNull(value, "AthleteSportId value must not be null");
		return new AthleteSportId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AthleteSportId athleteSportId)) {
			return false;
		}
		return value.equals(athleteSportId.value);
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
