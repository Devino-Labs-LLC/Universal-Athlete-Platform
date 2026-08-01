package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyRecoveryCheckInRevisionResult;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

record DailyRecoveryCheckInRevisionResponse(
		UUID id,
		UUID recoveryCheckInId,
		int revisionNumber,
		Integer priorSleepDurationMinutes,
		Integer newSleepDurationMinutes,
		RatingResponse priorSleepQuality,
		RatingResponse newSleepQuality,
		RatingResponse priorFatigue,
		RatingResponse newFatigue,
		RatingResponse priorMuscleSoreness,
		RatingResponse newMuscleSoreness,
		RatingResponse priorStress,
		RatingResponse newStress,
		RatingResponse priorMood,
		RatingResponse newMood,
		RatingResponse priorMotivation,
		RatingResponse newMotivation,
		RecoveryCheckInCompleteness priorCompleteness,
		RecoveryCheckInCompleteness newCompleteness,
		String priorNotes,
		String newNotes,
		List<BodyAreaDiscomfortResponse> priorDiscomfort,
		List<BodyAreaDiscomfortResponse> newDiscomfort,
		Instant changedAt,
		Instant createdAt) {

	static DailyRecoveryCheckInRevisionResponse from(DailyRecoveryCheckInRevisionResult result) {
		return new DailyRecoveryCheckInRevisionResponse(
				result.id().value(),
				result.recoveryCheckInId().value(),
				result.revisionNumber(),
				result.priorSleepDurationMinutes(),
				result.newSleepDurationMinutes(),
				result.priorSleepQuality() == null ? null : RatingResponse.from(result.priorSleepQuality()),
				result.newSleepQuality() == null ? null : RatingResponse.from(result.newSleepQuality()),
				RatingResponse.from(result.priorFatigue()),
				RatingResponse.from(result.newFatigue()),
				RatingResponse.from(result.priorMuscleSoreness()),
				RatingResponse.from(result.newMuscleSoreness()),
				RatingResponse.from(result.priorStress()),
				RatingResponse.from(result.newStress()),
				RatingResponse.from(result.priorMood()),
				RatingResponse.from(result.newMood()),
				RatingResponse.from(result.priorMotivation()),
				RatingResponse.from(result.newMotivation()),
				result.priorCompleteness(),
				result.newCompleteness(),
				result.priorNotes(),
				result.newNotes(),
				result.priorDiscomfort().stream().map(BodyAreaDiscomfortResponse::from).toList(),
				result.newDiscomfort().stream().map(BodyAreaDiscomfortResponse::from).toList(),
				result.changedAt(),
				result.createdAt());
	}

}

record DailyRecoveryCheckInRevisionListResponse(List<DailyRecoveryCheckInRevisionResponse> revisions) {
}
