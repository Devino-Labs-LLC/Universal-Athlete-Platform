package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class ExerciseSubstitutionRelationshipId {

	private final UUID value;

	private ExerciseSubstitutionRelationshipId(UUID value) {
		this.value = Objects.requireNonNull(value, "ExerciseSubstitutionRelationshipId value must not be null");
	}

	public static ExerciseSubstitutionRelationshipId generate() {
		return new ExerciseSubstitutionRelationshipId(UUID.randomUUID());
	}

	public static ExerciseSubstitutionRelationshipId of(UUID value) {
		return new ExerciseSubstitutionRelationshipId(value);
	}

	public static ExerciseSubstitutionRelationshipId of(String value) {
		Objects.requireNonNull(value, "ExerciseSubstitutionRelationshipId value must not be null");
		return new ExerciseSubstitutionRelationshipId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ExerciseSubstitutionRelationshipId that)) {
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
