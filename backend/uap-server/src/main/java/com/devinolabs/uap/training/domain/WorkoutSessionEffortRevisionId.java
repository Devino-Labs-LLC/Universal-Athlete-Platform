package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutSessionEffortRevisionId {

	private final UUID value;

	private WorkoutSessionEffortRevisionId(UUID value) {
		this.value = Objects.requireNonNull(value, "WorkoutSessionEffortRevisionId value must not be null");
	}

	public static WorkoutSessionEffortRevisionId generate() {
		return new WorkoutSessionEffortRevisionId(UUID.randomUUID());
	}

	public static WorkoutSessionEffortRevisionId of(UUID value) {
		return new WorkoutSessionEffortRevisionId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutSessionEffortRevisionId that)) {
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
