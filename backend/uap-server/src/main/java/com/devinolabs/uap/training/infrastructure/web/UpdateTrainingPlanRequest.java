package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTrainingPlanRequest(
		PatchValue<String> name,
		PatchValue<String> description,
		PatchValue<LocalDate> startDate,
		PatchValue<LocalDate> endDate,
		PatchValue<UUID> athleteSportId,
		PatchValue<UUID> athleteGoalId,
		PatchValue<UUID> defaultTrainingEnvironmentId) {
}
