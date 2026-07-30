package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;

/**
 * Factual equipment match between an exercise definition and a training environment.
 */
public record ExerciseEnvironmentCompatibility(
		boolean compatible,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> availableEquipment,
		List<EquipmentType> missingRequiredEquipment) {

	public ExerciseEnvironmentCompatibility {
		Objects.requireNonNull(requiredEquipment, "requiredEquipment must not be null");
		Objects.requireNonNull(availableEquipment, "availableEquipment must not be null");
		Objects.requireNonNull(missingRequiredEquipment, "missingRequiredEquipment must not be null");
		requiredEquipment = List.copyOf(requiredEquipment);
		availableEquipment = List.copyOf(availableEquipment);
		missingRequiredEquipment = List.copyOf(missingRequiredEquipment);
	}

}
