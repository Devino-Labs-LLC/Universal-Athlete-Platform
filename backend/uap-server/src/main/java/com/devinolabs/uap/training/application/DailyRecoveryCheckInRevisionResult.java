package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevision;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevisionId;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

public record DailyRecoveryCheckInRevisionResult(
		DailyRecoveryCheckInRevisionId id,
		DailyRecoveryCheckInId recoveryCheckInId,
		AthleteId athleteId,
		int revisionNumber,
		Integer priorSleepDurationMinutes,
		Integer newSleepDurationMinutes,
		SleepQualityRating priorSleepQuality,
		SleepQualityRating newSleepQuality,
		FatigueRating priorFatigue,
		FatigueRating newFatigue,
		MuscleSorenessRating priorMuscleSoreness,
		MuscleSorenessRating newMuscleSoreness,
		StressRating priorStress,
		StressRating newStress,
		MoodRating priorMood,
		MoodRating newMood,
		TrainingMotivationRating priorMotivation,
		TrainingMotivationRating newMotivation,
		RecoveryCheckInCompleteness priorCompleteness,
		RecoveryCheckInCompleteness newCompleteness,
		String priorNotes,
		String newNotes,
		List<BodyAreaDiscomfortObservation> priorDiscomfort,
		List<BodyAreaDiscomfortObservation> newDiscomfort,
		Instant changedAt,
		Instant createdAt) {

	public static DailyRecoveryCheckInRevisionResult from(DailyRecoveryCheckInRevision revision) {
		return new DailyRecoveryCheckInRevisionResult(
				revision.id(),
				revision.recoveryCheckInId(),
				revision.athleteId(),
				revision.revisionNumber(),
				revision.priorSleepDurationMinutes(),
				revision.newSleepDurationMinutes(),
				revision.priorSleepQuality(),
				revision.newSleepQuality(),
				revision.priorFatigue(),
				revision.newFatigue(),
				revision.priorMuscleSoreness(),
				revision.newMuscleSoreness(),
				revision.priorStress(),
				revision.newStress(),
				revision.priorMood(),
				revision.newMood(),
				revision.priorMotivation(),
				revision.newMotivation(),
				revision.priorCompleteness(),
				revision.newCompleteness(),
				revision.priorNotes(),
				revision.newNotes(),
				revision.priorDiscomfort(),
				revision.newDiscomfort(),
				revision.changedAt(),
				revision.createdAt());
	}

}
