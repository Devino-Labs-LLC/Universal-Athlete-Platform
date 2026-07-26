package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;

public record AthleteTrainingTodayResult(
		LocalDate date,
		String timezone,
		List<AthleteCalendarEntryResult> entries) {
}
