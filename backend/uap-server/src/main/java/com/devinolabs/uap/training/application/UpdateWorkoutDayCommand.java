package com.devinolabs.uap.training.application;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateWorkoutDayCommand(
		String title,
		boolean titlePresent,
		String description,
		boolean descriptionPresent,
		Integer planWeekNumber,
		boolean planWeekNumberPresent,
		DayOfWeek scheduledDayOfWeek,
		boolean scheduledDayOfWeekPresent,
		LocalTime plannedStartTime,
		boolean plannedStartTimePresent,
		Integer expectedDurationMinutes,
		boolean expectedDurationMinutesPresent,
		Integer displayOrder,
		boolean displayOrderPresent) {

	public boolean touchesPlacement() {
		return planWeekNumberPresent || scheduledDayOfWeekPresent || plannedStartTimePresent;
	}

}
