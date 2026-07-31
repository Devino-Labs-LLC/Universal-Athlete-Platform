package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class WorkoutAdaptationProposalId {

	private final UUID value;

	private WorkoutAdaptationProposalId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static WorkoutAdaptationProposalId of(UUID value) {
		return new WorkoutAdaptationProposalId(value);
	}

	public static WorkoutAdaptationProposalId of(String value) {
		return of(UUID.fromString(value));
	}

	public static WorkoutAdaptationProposalId generate() {
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
		if (!(other instanceof WorkoutAdaptationProposalId that)) {
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
