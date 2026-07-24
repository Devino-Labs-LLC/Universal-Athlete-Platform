package com.devinolabs.uap.training.application;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutDayStatus;

public record WorkoutDayResult(
		WorkoutDayId id,
		int displayOrder,
		String title,
		String description,
		DayOfWeek scheduledDay,
		LocalTime plannedStartTime,
		Integer expectedDurationMinutes,
		WorkoutDayStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
