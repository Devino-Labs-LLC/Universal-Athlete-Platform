package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record WorkoutOccurrenceResult(
		WorkoutOccurrenceId id,
		WorkoutDayId workoutDayId,
		LocalDate scheduledDate,
		LocalTime plannedStartTime,
		Instant startedAt,
		Instant completedAt,
		WorkoutOccurrenceStatus status,
		String athleteNotes,
		Instant createdAt,
		Instant updatedAt) {
}
