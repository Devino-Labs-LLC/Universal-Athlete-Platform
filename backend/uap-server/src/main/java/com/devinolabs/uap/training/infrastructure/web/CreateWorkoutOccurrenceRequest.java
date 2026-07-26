package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkoutOccurrenceRequest(
		@NotNull LocalDate scheduledDate,
		LocalTime plannedStartTime,
		@Size(max = 4000) String athleteNotes) {
}
