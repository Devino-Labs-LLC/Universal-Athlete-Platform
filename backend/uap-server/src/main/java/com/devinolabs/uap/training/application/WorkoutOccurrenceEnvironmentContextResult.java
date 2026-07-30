package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public record WorkoutOccurrenceEnvironmentContextResult(
		TrainingEnvironmentId trainingEnvironmentId,
		String nameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot,
		Instant selectedAt) {
}
