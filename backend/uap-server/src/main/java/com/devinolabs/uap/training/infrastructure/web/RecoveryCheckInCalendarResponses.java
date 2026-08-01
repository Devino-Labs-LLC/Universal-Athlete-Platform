package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.devinolabs.uap.training.application.RecoveryTrainingLoadContextResult;

record RecoveryTrainingLoadContextResponse(
		LocalDate date,
		long occurrenceCount,
		long ratedOccurrenceCount,
		long unratedOccurrenceCount,
		long completedExerciseCount,
		long completedSetCount,
		BigDecimal totalVolumeKilograms,
		long totalDurationSeconds,
		BigDecimal totalDistanceMeters,
		BigDecimal totalSessionRpeLoad) {

	static RecoveryTrainingLoadContextResponse from(RecoveryTrainingLoadContextResult result) {
		if (result == null) {
			return null;
		}
		return new RecoveryTrainingLoadContextResponse(
				result.date(),
				result.occurrenceCount(),
				result.ratedOccurrenceCount(),
				result.unratedOccurrenceCount(),
				result.completedExerciseCount(),
				result.completedSetCount(),
				result.totalVolumeKilograms(),
				result.totalDurationSeconds(),
				result.totalDistanceMeters(),
				result.totalSessionRpeLoad());
	}

}

record RecoveryCheckInCalendarDayResponse(
		LocalDate date,
		boolean checkInPresent,
		DailyRecoveryCheckInResponse checkIn,
		long scheduledWorkoutCount,
		long completedWorkoutCount,
		RecoveryTrainingLoadContextResponse trainingLoad) {

	static RecoveryCheckInCalendarDayResponse from(
			com.devinolabs.uap.training.application.RecoveryCheckInCalendarDayResult result) {
		return new RecoveryCheckInCalendarDayResponse(
				result.date(),
				result.checkInPresent(),
				result.checkIn() == null ? null : DailyRecoveryCheckInResponse.from(result.checkIn()),
				result.scheduledWorkoutCount(),
				result.completedWorkoutCount(),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoad()));
	}

}

record RecoveryCheckInCalendarResponse(
		java.util.List<RecoveryCheckInCalendarDayResponse> days) {

	static RecoveryCheckInCalendarResponse from(
			com.devinolabs.uap.training.application.RecoveryCheckInCalendarResult result) {
		return new RecoveryCheckInCalendarResponse(
				result.days().stream().map(RecoveryCheckInCalendarDayResponse::from).toList());
	}

}

record AthleteRecoveryHistoryDayResponse(
		LocalDate date,
		DailyRecoveryCheckInResponse checkIn,
		RecoveryTrainingLoadContextResponse trainingLoad,
		int revisionCount,
		java.time.Instant lastUpdatedAt) {

	static AthleteRecoveryHistoryDayResponse from(
			com.devinolabs.uap.training.application.AthleteRecoveryHistoryDayResult result) {
		return new AthleteRecoveryHistoryDayResponse(
				result.date(),
				DailyRecoveryCheckInResponse.from(result.checkIn()),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoad()),
				result.revisionCount(),
				result.lastUpdatedAt());
	}

}

record AthleteRecoveryHistoryResponse(java.util.List<AthleteRecoveryHistoryDayResponse> days) {

	static AthleteRecoveryHistoryResponse from(
			com.devinolabs.uap.training.application.AthleteRecoveryHistoryResult result) {
		return new AthleteRecoveryHistoryResponse(
				result.days().stream().map(AthleteRecoveryHistoryDayResponse::from).toList());
	}

}
