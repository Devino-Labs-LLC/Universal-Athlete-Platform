package com.devinolabs.uap.training.infrastructure.web;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkoutDayRequest(
		@NotBlank @Size(max = 160) String title,
		@Size(max = 2000) String description,
		@NotNull @Min(1) Integer planWeekNumber,
		@NotNull DayOfWeek scheduledDayOfWeek,
		LocalTime plannedStartTime,
		@Min(1) Integer expectedDurationMinutes,
		@Min(0) Integer displayOrder,
		java.util.UUID trainingEnvironmentOverrideId) {
}
