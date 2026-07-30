package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.WorkoutOccurrenceEnvironmentDetailResult;

public record WorkoutOccurrenceEnvironmentContextResponse(
		WorkoutOccurrenceEnvironmentSnapshotResponse plannedEnvironment,
		WorkoutOccurrenceEnvironmentSnapshotResponse actualEnvironment) {

	static WorkoutOccurrenceEnvironmentContextResponse from(WorkoutOccurrenceEnvironmentDetailResult result) {
		if (result == null) {
			return null;
		}
		return new WorkoutOccurrenceEnvironmentContextResponse(
				WorkoutOccurrenceEnvironmentSnapshotResponse.from(result.plannedEnvironment()),
				WorkoutOccurrenceEnvironmentSnapshotResponse.from(result.actualEnvironment()));
	}
}
