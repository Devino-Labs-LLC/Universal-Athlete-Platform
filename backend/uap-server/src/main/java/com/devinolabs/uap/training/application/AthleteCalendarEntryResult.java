package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record AthleteCalendarEntryResult(
		WorkoutOccurrenceId occurrenceId,
		TrainingPlanId trainingPlanId,
		String trainingPlanName,
		WorkoutDayId workoutDayId,
		String workoutDayName,
		LocalDate scheduledDate,
		LocalTime plannedStartTime,
		WorkoutOccurrenceStatus status,
		WorkoutOccurrenceOrigin origin,
		boolean manuallyRescheduled,
		LocalDate originalScheduledDate,
		Instant startedAt,
		Instant completedAt,
		String athleteNotes,
		int exerciseCount,
		int notStartedExerciseCount,
		int inProgressExerciseCount,
		int completedExerciseCount,
		int skippedExerciseCount) {
}
