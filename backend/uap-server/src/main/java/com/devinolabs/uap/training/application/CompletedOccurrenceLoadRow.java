package com.devinolabs.uap.training.application;

import java.time.LocalDate;

import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record CompletedOccurrenceLoadRow(
		WorkoutOccurrence occurrence,
		WorkoutOccurrenceId occurrenceId,
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		LocalDate scheduledDate) {
}
