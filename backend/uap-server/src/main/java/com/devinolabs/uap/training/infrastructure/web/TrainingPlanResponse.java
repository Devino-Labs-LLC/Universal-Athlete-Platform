package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

public record TrainingPlanResponse(
		UUID id,
		TrainingPlanType type,
		String customTypeName,
		String name,
		String description,
		TrainingPlanStatus status,
		LocalDate startDate,
		LocalDate endDate,
		UUID athleteSportId,
		UUID athleteGoalId,
		Instant createdAt,
		Instant updatedAt) {
}
