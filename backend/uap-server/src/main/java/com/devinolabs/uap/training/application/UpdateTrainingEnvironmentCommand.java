package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record UpdateTrainingEnvironmentCommand(
		String name,
		boolean namePresent,
		TrainingEnvironmentType type,
		boolean typePresent,
		List<EquipmentType> availableEquipment,
		boolean availableEquipmentPresent,
		String description,
		boolean descriptionPresent,
		String facilityNotes,
		boolean facilityNotesPresent,
		Boolean defaultEnvironment,
		boolean defaultEnvironmentPresent) {
}
