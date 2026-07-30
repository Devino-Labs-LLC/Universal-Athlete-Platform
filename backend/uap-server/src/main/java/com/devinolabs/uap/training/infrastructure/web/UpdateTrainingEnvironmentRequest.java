package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record UpdateTrainingEnvironmentRequest(
		PatchValue<String> name,
		PatchValue<TrainingEnvironmentType> type,
		PatchValue<List<EquipmentType>> availableEquipment,
		PatchValue<String> description,
		PatchValue<String> facilityNotes,
		PatchValue<Boolean> defaultEnvironment) {
}
