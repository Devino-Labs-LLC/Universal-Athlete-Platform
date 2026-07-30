package com.devinolabs.uap.training.application;

public record WorkoutOccurrenceEnvironmentDetailResult(
		WorkoutOccurrenceEnvironmentContextResult plannedEnvironment,
		WorkoutOccurrenceEnvironmentContextResult actualEnvironment) {
}
