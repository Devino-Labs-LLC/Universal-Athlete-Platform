package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.training.domain.AthleteGoalId;
import com.devinolabs.uap.training.domain.AthleteSportId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingPlanScheduleStatus;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

public record TrainingPlanResult(
		TrainingPlanId id,
		TrainingPlanType type,
		String customTypeName,
		String name,
		String description,
		TrainingPlanStatus status,
		LocalDate startDate,
		LocalDate endDate,
		AthleteSportId athleteSportId,
		AthleteGoalId athleteGoalId,
		TrainingEnvironmentId defaultTrainingEnvironmentId,
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
