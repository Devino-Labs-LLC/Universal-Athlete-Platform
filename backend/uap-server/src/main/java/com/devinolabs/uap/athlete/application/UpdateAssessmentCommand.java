package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentCommand(
		String title,
		boolean titlePresent,
		String description,
		boolean descriptionPresent,
		String notes,
		boolean notesPresent,
		Instant scheduledAt,
		boolean scheduledAtPresent,
		UUID athleteSportId,
		boolean athleteSportIdPresent,
		UUID athleteGoalId,
		boolean athleteGoalIdPresent) {
}
