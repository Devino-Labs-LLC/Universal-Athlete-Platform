package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.application.ExerciseCompatibilityDetailResult;
import com.devinolabs.uap.training.domain.EquipmentType;

record ExerciseCompatibilityDetailResponse(
		boolean compatible,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> availableEquipment,
		List<EquipmentType> missingRequiredEquipment) {

	static ExerciseCompatibilityDetailResponse from(ExerciseCompatibilityDetailResult result) {
		return new ExerciseCompatibilityDetailResponse(
				result.compatible(),
				result.requiredEquipment(),
				result.availableEquipment(),
				result.missingRequiredEquipment());
	}

}
