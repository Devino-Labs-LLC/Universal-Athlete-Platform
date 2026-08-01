package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.RecoveryCheckInSource;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

public record DailyRecoveryCheckInResult(
		DailyRecoveryCheckInId id,
		AthleteId athleteId,
		LocalDate checkInDate,
		Integer sleepDurationMinutes,
		SleepQualityRating sleepQuality,
		FatigueRating fatigue,
		MuscleSorenessRating muscleSoreness,
		StressRating stress,
		MoodRating mood,
		TrainingMotivationRating motivation,
		RecoveryCheckInCompleteness completeness,
		List<BodyAreaDiscomfortObservation> discomfortAreas,
		String notes,
		RecoveryCheckInSource source,
		Instant submittedAt,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public static DailyRecoveryCheckInResult from(DailyRecoveryCheckIn checkIn) {
		return new DailyRecoveryCheckInResult(
				checkIn.id(),
				checkIn.athleteId(),
				checkIn.checkInDate(),
				checkIn.sleepDurationMinutes(),
				checkIn.sleepQuality(),
				checkIn.fatigue(),
				checkIn.muscleSoreness(),
				checkIn.stress(),
				checkIn.mood(),
				checkIn.motivation(),
				checkIn.completeness(),
				checkIn.discomfortAreas(),
				checkIn.notes(),
				checkIn.source(),
				checkIn.submittedAt(),
				checkIn.createdAt(),
				checkIn.updatedAt(),
				checkIn.version());
	}

}
