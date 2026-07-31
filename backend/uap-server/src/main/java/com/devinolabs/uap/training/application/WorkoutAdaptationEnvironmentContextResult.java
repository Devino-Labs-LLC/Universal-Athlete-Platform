package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public record WorkoutAdaptationEnvironmentContextResult(
		FeasibilityEnvironmentContextSource contextSource,
		TrainingEnvironmentId trainingEnvironmentId,
		String environmentNameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot) {

	public WorkoutAdaptationEnvironmentContextResult {
		availableEquipmentSnapshot = availableEquipmentSnapshot == null
				? List.of()
				: List.copyOf(availableEquipmentSnapshot);
	}

}
