package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.TrainingEnvironmentResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record TrainingEnvironmentResponse(
		UUID id,
		UUID athleteId,
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

	static TrainingEnvironmentResponse from(TrainingEnvironmentResult result) {
		return new TrainingEnvironmentResponse(
				result.id().value(),
				result.athleteId().value(),
				result.name(),
				result.type(),
				result.availableEquipment(),
				result.description(),
				result.facilityNotes(),
				result.defaultEnvironment(),
				result.active(),
				result.archivedAt(),
				result.createdAt(),
				result.updatedAt());
	}
}
