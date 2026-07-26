package com.devinolabs.uap.training.application;

import java.time.LocalDate;

import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;

/**
 * @param generateThrough optional initial horizon; when absent the schedule is activated only and
 * occurrences are materialised by a separate generate call
 */
public record ActivateTrainingPlanScheduleCommand(
		LocalDate scheduleStartDate,
		LocalDate scheduleEndDate,
		String timezone,
		TrainingPlanRecurrenceMode recurrenceMode,
		LocalDate generateThrough) {
}
