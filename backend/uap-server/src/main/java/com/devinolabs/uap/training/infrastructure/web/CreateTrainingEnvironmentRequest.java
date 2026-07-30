package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record CreateTrainingEnvironmentRequest(
		@NotBlank @Size(min = 2, max = 100) String name,
		@NotNull TrainingEnvironmentType type,
		List<EquipmentType> availableEquipment,
		@Size(max = 2000) String description,
		@Size(max = 2000) String facilityNotes,
		Boolean defaultEnvironment) {
}
