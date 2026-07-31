package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public record FeasibilityEnvironmentContextResult(
		TrainingEnvironmentId trainingEnvironmentId,
		String trainingEnvironmentName,
		List<EquipmentType> availableEquipment,
		FeasibilityEnvironmentContextSource contextSource,
		Instant snapshotCapturedAt) {

	public FeasibilityEnvironmentContextResult {
		availableEquipment = availableEquipment == null ? List.of() : List.copyOf(availableEquipment);
	}

}
