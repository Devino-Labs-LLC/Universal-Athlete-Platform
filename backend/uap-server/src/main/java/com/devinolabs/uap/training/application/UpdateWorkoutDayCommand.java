package com.devinolabs.uap.training.application;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateWorkoutDayCommand(
		String title,
		boolean titlePresent,
		String description,
		boolean descriptionPresent,
		DayOfWeek scheduledDay,
		boolean scheduledDayPresent,
		LocalTime plannedStartTime,
		boolean plannedStartTimePresent,
		Integer expectedDurationMinutes,
		boolean expectedDurationMinutesPresent,
		Integer displayOrder,
		boolean displayOrderPresent) {
}
