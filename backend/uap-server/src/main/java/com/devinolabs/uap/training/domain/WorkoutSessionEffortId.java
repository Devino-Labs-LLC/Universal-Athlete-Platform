package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutSessionEffortId {

	private final UUID value;

	private WorkoutSessionEffortId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutSessionEffortId value must not be null");
	}

	public static WorkoutSessionEffortId generate() {
		return new WorkoutSessionEffortId(UUID.randomUUID());
	}

	public static WorkoutSessionEffortId of(UUID value) {
		return new WorkoutSessionEffortId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutSessionEffortId that)) {
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
