package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;

public record AssessmentResponse(
		UUID id,
		AssessmentType type,
		String customTypeName,
		String title,
		String description,
		AssessmentStatus status,
		Instant scheduledAt,
		Instant startedAt,
		Instant completedAt,
		String notes,
		UUID athleteSportId,
		UUID athleteGoalId,
		Instant createdAt,
		Instant updatedAt) {
}
