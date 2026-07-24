package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class TrainingPlanId {

	private final UUID value;

	private TrainingPlanId(UUID value) {
		this.value = Objects.requireNonNull(value, "TrainingPlanId value must not be null");
	}

	public static TrainingPlanId generate() {
		return new TrainingPlanId(UUID.randomUUID());
	}

	public static TrainingPlanId of(UUID value) {
		return new TrainingPlanId(value);
	}

	public static TrainingPlanId of(String value) {
		Objects.requireNonNull(value, "TrainingPlanId value must not be null");
		return new TrainingPlanId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TrainingPlanId trainingPlanId)) {
			return false;
		}
		return value.equals(trainingPlanId.value);
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
