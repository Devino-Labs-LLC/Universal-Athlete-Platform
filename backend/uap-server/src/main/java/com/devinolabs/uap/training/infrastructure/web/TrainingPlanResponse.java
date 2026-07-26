package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingPlanScheduleStatus;
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
		LocalDate scheduleStartDate,
		LocalDate scheduleEndDate,
		String scheduleTimezone,
		TrainingPlanScheduleStatus scheduleStatus,
		TrainingPlanRecurrenceMode recurrenceMode,
		LocalDate scheduleGeneratedThrough,
		Instant scheduleActivatedAt,
		Instant schedulePausedAt,
		Instant createdAt,
		Instant updatedAt) {
}
