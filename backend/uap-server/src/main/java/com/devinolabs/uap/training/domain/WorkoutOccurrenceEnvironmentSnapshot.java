package com.devinolabs.uap.training.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable historical environment context captured on a workout occurrence.
 */
public final class WorkoutOccurrenceEnvironmentSnapshot {

	private final TrainingEnvironmentId trainingEnvironmentId;
	private final String nameSnapshot;
	private final List<EquipmentType> availableEquipmentSnapshot;

	private WorkoutOccurrenceEnvironmentSnapshot(
			TrainingEnvironmentId trainingEnvironmentId,
			String nameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot) {
		this.trainingEnvironmentId = Objects.requireNonNull(
				trainingEnvironmentId, "trainingEnvironmentId must not be null");
		this.nameSnapshot = Objects.requireNonNull(nameSnapshot, "nameSnapshot must not be null");
		this.availableEquipmentSnapshot = List.copyOf(availableEquipmentSnapshot);
	}

	public static WorkoutOccurrenceEnvironmentSnapshot of(
			TrainingEnvironmentId trainingEnvironmentId,
			String nameSnapshot,
			Collection<EquipmentType> availableEquipmentSnapshot) {
		Objects.requireNonNull(nameSnapshot, "nameSnapshot must not be null");
		String trimmed = nameSnapshot.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("nameSnapshot must not be blank");
		}
		List<EquipmentType> equipment = availableEquipmentSnapshot == null
				? List.of()
				: availableEquipmentSnapshot.stream()
						.filter(Objects::nonNull)
						.distinct()
						.sorted(Comparator.comparingInt(Enum::ordinal))
						.toList();
		return new WorkoutOccurrenceEnvironmentSnapshot(trainingEnvironmentId, trimmed, equipment);
	}

	public static WorkoutOccurrenceEnvironmentSnapshot from(TrainingEnvironment environment) {
		Objects.requireNonNull(environment, "environment must not be null");
		return of(environment.id(), environment.name(), environment.availableEquipment());
	}

	public TrainingEnvironmentId trainingEnvironmentId() {
		return trainingEnvironmentId;
	}

	public String nameSnapshot() {
		return nameSnapshot;
	}

	public List<EquipmentType> availableEquipmentSnapshot() {
		return availableEquipmentSnapshot;
	}

}
