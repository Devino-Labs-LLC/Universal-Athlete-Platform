package com.devinolabs.uap.training.infrastructure.web;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateWorkoutDayRequest(
		PatchValue<String> title,
		PatchValue<String> description,
		PatchValue<DayOfWeek> scheduledDay,
		PatchValue<LocalTime> plannedStartTime,
		PatchValue<Integer> expectedDurationMinutes,
		PatchValue<Integer> displayOrder) {
}
