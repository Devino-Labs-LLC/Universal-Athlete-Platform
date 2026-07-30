package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class TrainingEnvironmentId {

	private final UUID value;

	private TrainingEnvironmentId(UUID value) {
		this.value = Objects.requireNonNull(value, "TrainingEnvironmentId value must not be null");
	}

	public static TrainingEnvironmentId generate() {
		return new TrainingEnvironmentId(UUID.randomUUID());
	}

	public static TrainingEnvironmentId of(UUID value) {
		return new TrainingEnvironmentId(value);
	}

	public static TrainingEnvironmentId of(String value) {
		Objects.requireNonNull(value, "TrainingEnvironmentId value must not be null");
		return new TrainingEnvironmentId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TrainingEnvironmentId that)) {
			return false;
		}
		return value.equals(that.value);
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
