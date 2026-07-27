package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identity an exercise's performance history is aggregated under.
 *
 * <p>The key wraps an {@link ExerciseDefinitionId}: the canonical movement, not the prescription
 * row. Keying on the definition means every plan that prescribes the same movement contributes to
 * one history and one set of personal records, and renaming or deleting a prescription changes
 * nothing. The key and the definition id always hold the same UUID value, so a definition id can be
 * used wherever an API exposes a performance key.
 */
public final class ExercisePerformanceKey {

	private final UUID value;

	private ExercisePerformanceKey(UUID value) {
		this.value = Objects.requireNonNull(value, "ExercisePerformanceKey value must not be null");
	}

	public static ExercisePerformanceKey of(ExerciseDefinitionId exerciseDefinitionId) {
		Objects.requireNonNull(exerciseDefinitionId, "exerciseDefinitionId must not be null");
		return new ExercisePerformanceKey(exerciseDefinitionId.value());
	}

	public static ExercisePerformanceKey of(UUID value) {
		return new ExercisePerformanceKey(value);
	}

	public static ExercisePerformanceKey fromString(String value) {
		Objects.requireNonNull(value, "ExercisePerformanceKey value must not be null");
		return new ExercisePerformanceKey(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	public ExerciseDefinitionId toDefinitionId() {
		return ExerciseDefinitionId.of(value);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ExercisePerformanceKey exercisePerformanceKey)) {
			return false;
		}
		return value.equals(exercisePerformanceKey.value);
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
