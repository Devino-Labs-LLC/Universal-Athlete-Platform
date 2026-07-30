package com.devinolabs.uap.training.application;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutDayStatus;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public record WorkoutDayResult(
		WorkoutDayId id,
		int displayOrder,
		String title,
		String description,
		Integer planWeekNumber,
		DayOfWeek scheduledDayOfWeek,
		LocalTime plannedStartTime,
		Integer expectedDurationMinutes,
		WorkoutDayStatus status,
		TrainingEnvironmentId trainingEnvironmentOverrideId,
		Instant createdAt,
		Instant updatedAt) {
}
