package com.devinolabs.uap.athlete.application;

import java.time.Instant;

import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

public record AssessmentResult(
		AssessmentId id,
		AssessmentType type,
		String customTypeName,
		String title,
		String description,
		AssessmentStatus status,
		Instant scheduledAt,
		Instant startedAt,
		Instant completedAt,
		String notes,
		AthleteSportId athleteSportId,
		AthleteGoalId athleteGoalId,
		Instant createdAt,
		Instant updatedAt) {
}
