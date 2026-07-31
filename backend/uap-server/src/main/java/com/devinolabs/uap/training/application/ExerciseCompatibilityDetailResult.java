package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;

public record ExerciseCompatibilityDetailResult(
		boolean compatible,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> availableEquipment,
		List<EquipmentType> missingRequiredEquipment) {

	public ExerciseCompatibilityDetailResult {
		requiredEquipment = requiredEquipment == null ? List.of() : List.copyOf(requiredEquipment);
		availableEquipment = availableEquipment == null ? List.of() : List.copyOf(availableEquipment);
		missingRequiredEquipment = missingRequiredEquipment == null ? List.of() : List.copyOf(missingRequiredEquipment);
	}

	static ExerciseCompatibilityDetailResult from(
			com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibility compatibility) {
		return new ExerciseCompatibilityDetailResult(
				compatibility.compatible(),
				compatibility.requiredEquipment(),
				compatibility.availableEquipment(),
				compatibility.missingRequiredEquipment());
	}

}
