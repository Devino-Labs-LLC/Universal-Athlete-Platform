package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

record RescheduleWorkoutOccurrenceRequest(
		@NotNull LocalDate scheduledDate,
		LocalTime plannedStartTime) {
}
