package com.devinolabs.uap.training.application;

public record TrainingPlanScheduleActivationResult(
		TrainingPlanResult plan,
		WorkoutOccurrenceGenerationResult generation) {
}
