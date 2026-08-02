package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BodyAreaDiscomfortHistoryResult(
		LocalDate startDate,
		LocalDate endDate,
		int observationCount,
		int datesObserved,
		BigDecimal averageIntensity,
		Integer maximumIntensity,
		LocalDate latestObservationDate,
		List<BodyAreaDiscomfortHistoryEntryResult> entries) {

}
