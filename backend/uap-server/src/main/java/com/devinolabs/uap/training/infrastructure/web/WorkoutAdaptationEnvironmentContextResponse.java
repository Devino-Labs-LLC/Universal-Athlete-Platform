package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationEnvironmentContextResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;

record WorkoutAdaptationEnvironmentContextResponse(
		FeasibilityEnvironmentContextSource contextSource,
		UUID trainingEnvironmentId,
		String environmentNameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot) {

	static WorkoutAdaptationEnvironmentContextResponse from(WorkoutAdaptationEnvironmentContextResult result) {
		if (result == null) {
			return null;
		}
		return new WorkoutAdaptationEnvironmentContextResponse(
				result.contextSource(),
				result.trainingEnvironmentId() == null ? null : result.trainingEnvironmentId().value(),
				result.environmentNameSnapshot(),
				result.availableEquipmentSnapshot());
	}

}
