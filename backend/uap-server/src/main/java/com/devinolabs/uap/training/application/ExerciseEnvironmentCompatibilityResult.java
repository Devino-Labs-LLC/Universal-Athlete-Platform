package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public record ExerciseEnvironmentCompatibilityResult(
		ExerciseDefinitionId exerciseDefinitionId,
		TrainingEnvironmentId trainingEnvironmentId,
		String trainingEnvironmentName,
		boolean compatible,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> availableEquipment,
		List<EquipmentType> missingRequiredEquipment) {
}
