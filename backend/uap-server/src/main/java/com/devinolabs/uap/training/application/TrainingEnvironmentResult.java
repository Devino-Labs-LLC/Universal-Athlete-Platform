package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record TrainingEnvironmentResult(
		TrainingEnvironmentId id,
		AthleteId athleteId,
		String name,
		TrainingEnvironmentType type,
		List<EquipmentType> availableEquipment,
		String description,
		String facilityNotes,
		boolean defaultEnvironment,
		boolean active,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {
}
