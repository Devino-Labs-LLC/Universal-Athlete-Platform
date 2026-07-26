package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateWorkoutOccurrenceRequest(
		PatchValue<LocalDate> scheduledDate,
		PatchValue<LocalTime> plannedStartTime,
		PatchValue<String> athleteNotes) {
}
