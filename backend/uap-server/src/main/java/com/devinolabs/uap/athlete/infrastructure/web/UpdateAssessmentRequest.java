package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentRequest(
		PatchValue<String> title,
		PatchValue<String> description,
		PatchValue<String> notes,
		PatchValue<Instant> scheduledAt,
		PatchValue<UUID> athleteSportId,
		PatchValue<UUID> athleteGoalId) {
}
