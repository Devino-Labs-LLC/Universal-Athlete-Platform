package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecoveryTrainingLoadContextResult(
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

	public static RecoveryTrainingLoadContextResult empty(LocalDate date) {
		return new RecoveryTrainingLoadContextResult(
				date, 0L, 0L, 0L, 0L, 0L,
				BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
	}

}
