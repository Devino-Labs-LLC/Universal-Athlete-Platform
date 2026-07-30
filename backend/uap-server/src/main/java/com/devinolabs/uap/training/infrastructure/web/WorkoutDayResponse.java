package com.devinolabs.uap.training.infrastructure.web;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import com.devinolabs.uap.training.domain.WorkoutDayStatus;

public record WorkoutDayResponse(
		UUID id,
		int displayOrder,
		String title,
		String description,
		Integer planWeekNumber,
		DayOfWeek scheduledDayOfWeek,
		LocalTime plannedStartTime,
		Integer expectedDurationMinutes,
		WorkoutDayStatus status,
		UUID trainingEnvironmentOverrideId,
		Instant createdAt,
		Instant updatedAt) {
}
