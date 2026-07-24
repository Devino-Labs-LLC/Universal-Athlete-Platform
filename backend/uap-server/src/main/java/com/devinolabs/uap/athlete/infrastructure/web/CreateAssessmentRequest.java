package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.AssessmentType;

public record CreateAssessmentRequest(
		@NotNull AssessmentType type,
		@Size(max = 120) String customTypeName,
		@NotBlank @Size(max = 160) String title,
		@Size(max = 1000) String description,
		Instant scheduledAt,
		@Size(max = 2000) String notes,
		UUID athleteSportId,
		UUID athleteGoalId) {
}
