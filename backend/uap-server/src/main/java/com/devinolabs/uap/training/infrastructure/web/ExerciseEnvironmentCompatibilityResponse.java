package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseEnvironmentCompatibilityResult;
import com.devinolabs.uap.training.domain.EquipmentType;

public record ExerciseEnvironmentCompatibilityResponse(
		UUID exerciseDefinitionId,
		UUID trainingEnvironmentId,
		String trainingEnvironmentName,
		boolean compatible,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> availableEquipment,
		List<EquipmentType> missingRequiredEquipment) {

	static ExerciseEnvironmentCompatibilityResponse from(ExerciseEnvironmentCompatibilityResult result) {
		return new ExerciseEnvironmentCompatibilityResponse(
				result.exerciseDefinitionId().value(),
				result.trainingEnvironmentId().value(),
				result.trainingEnvironmentName(),
				result.compatible(),
				result.requiredEquipment(),
				result.availableEquipment(),
				result.missingRequiredEquipment());
	}
}
