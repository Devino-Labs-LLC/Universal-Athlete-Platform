package com.devinolabs.uap.training.application;

import java.time.LocalDate;

public record RecoveryCheckInCalendarDayResult(
		LocalDate date,
		boolean checkInPresent,
		DailyRecoveryCheckInResult checkIn,
		long scheduledWorkoutCount,
		long completedWorkoutCount,
		RecoveryTrainingLoadContextResult trainingLoad) {
}
