package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyRecoveryBaselineComparisonResult;

record DailyRecoveryBaselineComparisonResponse(
		UUID checkInId,
		LocalDate targetDate,
		boolean checkInPresent,
		int baselineWindowDays,
		List<RecoveryMetricDeviationResponse> metricComparisons,
		List<BodyAreaDiscomfortResponse> discomfortObservations,
		RecoveryTrainingLoadContextResponse trainingLoadContext,
		Instant calculatedAt) {

	static DailyRecoveryBaselineComparisonResponse from(DailyRecoveryBaselineComparisonResult result) {
		return new DailyRecoveryBaselineComparisonResponse(
				result.checkInId(),
				result.targetDate(),
				result.checkInPresent(),
				result.baselineWindowDays(),
				result.metricComparisons().stream().map(RecoveryMetricDeviationResponse::from).toList(),
				result.discomfortObservations().stream().map(BodyAreaDiscomfortResponse::from).toList(),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoadContext()),
				result.calculatedAt());
	}

}
