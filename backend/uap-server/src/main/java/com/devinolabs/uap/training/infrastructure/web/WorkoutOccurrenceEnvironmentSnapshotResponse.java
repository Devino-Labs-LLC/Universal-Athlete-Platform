package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutOccurrenceEnvironmentContextResult;
import com.devinolabs.uap.training.domain.EquipmentType;

public record WorkoutOccurrenceEnvironmentSnapshotResponse(
		UUID trainingEnvironmentId,
		String nameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot,
		Instant selectedAt) {

	static WorkoutOccurrenceEnvironmentSnapshotResponse from(WorkoutOccurrenceEnvironmentContextResult result) {
		if (result == null) {
			return null;
		}
		return new WorkoutOccurrenceEnvironmentSnapshotResponse(
				result.trainingEnvironmentId().value(),
				result.nameSnapshot(),
				result.availableEquipmentSnapshot(),
				result.selectedAt());
	}
}
