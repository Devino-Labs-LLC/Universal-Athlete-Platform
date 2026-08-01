package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;

public record AthleteRecoveryHistoryDayResult(
		LocalDate date,
		DailyRecoveryCheckInResult checkIn,
		RecoveryTrainingLoadContextResult trainingLoad,
		int revisionCount,
		Instant lastUpdatedAt) {
}
