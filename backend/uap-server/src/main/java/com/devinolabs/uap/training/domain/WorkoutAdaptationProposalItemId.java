package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutAdaptationProposalItemId {

	private final UUID value;

	private WorkoutAdaptationProposalItemId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static WorkoutAdaptationProposalItemId of(UUID value) {
		return new WorkoutAdaptationProposalItemId(value);
	}

	public static WorkoutAdaptationProposalItemId of(String value) {
		return of(UUID.fromString(value));
	}

	public static WorkoutAdaptationProposalItemId generate() {
		return of(UUID.randomUUID());
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorkoutAdaptationProposalItemId that)) {
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
