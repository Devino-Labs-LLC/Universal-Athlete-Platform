package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateWorkoutOccurrenceCommand(
		LocalDate scheduledDate,
		boolean scheduledDatePresent,
		LocalTime plannedStartTime,
		boolean plannedStartTimePresent,
		String athleteNotes,
		boolean athleteNotesPresent) {
}
