package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyRecoveryCheckInResult;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.RecoveryCheckInSource;

record DailyRecoveryCheckInResponse(
		UUID id,
		LocalDate checkInDate,
		Integer sleepDurationMinutes,
		RatingResponse sleepQuality,
		RatingResponse fatigue,
		RatingResponse muscleSoreness,
		RatingResponse stress,
		RatingResponse mood,
		RatingResponse motivation,
		RecoveryCheckInCompleteness completeness,
		List<BodyAreaDiscomfortResponse> discomfortAreas,
		String notes,
		RecoveryCheckInSource source,
		Instant submittedAt,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	static DailyRecoveryCheckInResponse from(DailyRecoveryCheckInResult result) {
		return new DailyRecoveryCheckInResponse(
				result.id().value(),
				result.checkInDate(),
				result.sleepDurationMinutes(),
				result.sleepQuality() == null ? null : RatingResponse.from(result.sleepQuality()),
				RatingResponse.from(result.fatigue()),
				RatingResponse.from(result.muscleSoreness()),
				RatingResponse.from(result.stress()),
				RatingResponse.from(result.mood()),
				RatingResponse.from(result.motivation()),
				result.completeness(),
				result.discomfortAreas().stream().map(BodyAreaDiscomfortResponse::from).toList(),
				result.notes(),
				result.source(),
				result.submittedAt(),
				result.createdAt(),
				result.updatedAt(),
				result.version());
	}

}

record DailyRecoveryCheckInListResponse(
		List<DailyRecoveryCheckInResponse> checkIns,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static DailyRecoveryCheckInListResponse from(
			com.devinolabs.uap.training.application.DailyRecoveryCheckInListResult result) {
		return new DailyRecoveryCheckInListResponse(
				result.checkIns().stream().map(DailyRecoveryCheckInResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}

}
