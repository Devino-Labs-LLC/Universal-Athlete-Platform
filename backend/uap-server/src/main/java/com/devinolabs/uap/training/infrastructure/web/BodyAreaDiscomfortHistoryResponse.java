package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.BodyAreaDiscomfortHistoryEntryResult;
import com.devinolabs.uap.training.application.BodyAreaDiscomfortHistoryResult;

record BodyAreaDiscomfortHistoryEntryResponse(
		LocalDate date,
		UUID checkInId,
		String bodyArea,
		String side,
		RatingResponse intensity,
		String notes,
		long checkInVersion) {

	static BodyAreaDiscomfortHistoryEntryResponse from(BodyAreaDiscomfortHistoryEntryResult result) {
		return new BodyAreaDiscomfortHistoryEntryResponse(
				result.date(),
				result.checkInId(),
				result.bodyArea().name(),
				result.side().name(),
				RatingResponse.from(result.intensity()),
				result.notes(),
				result.checkInVersion());
	}

}

record BodyAreaDiscomfortHistoryResponse(
		LocalDate startDate,
		LocalDate endDate,
		int observationCount,
		int datesObserved,
		BigDecimal averageIntensity,
		Integer maximumIntensity,
		LocalDate latestObservationDate,
		List<BodyAreaDiscomfortHistoryEntryResponse> entries) {

	static BodyAreaDiscomfortHistoryResponse from(BodyAreaDiscomfortHistoryResult result) {
		return new BodyAreaDiscomfortHistoryResponse(
				result.startDate(),
				result.endDate(),
				result.observationCount(),
				result.datesObserved(),
				result.averageIntensity(),
				result.maximumIntensity(),
				result.latestObservationDate(),
				result.entries().stream().map(BodyAreaDiscomfortHistoryEntryResponse::from).toList());
	}

}
