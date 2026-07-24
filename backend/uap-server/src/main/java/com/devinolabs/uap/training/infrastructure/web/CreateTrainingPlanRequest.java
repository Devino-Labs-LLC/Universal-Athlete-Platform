package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.training.domain.TrainingPlanType;

public record CreateTrainingPlanRequest(
		@NotNull TrainingPlanType type,
		@Size(max = 120) String customTypeName,
		@NotBlank @Size(max = 160) String name,
		@Size(max = 2000) String description,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		UUID athleteSportId,
		UUID athleteGoalId) {
}
