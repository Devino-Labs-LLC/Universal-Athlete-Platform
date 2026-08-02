package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;

public record DailyRecoveryBaselineComparisonResult(
		UUID checkInId,
		LocalDate targetDate,
		boolean checkInPresent,
		int baselineWindowDays,
		List<RecoveryMetricDeviationResult> metricComparisons,
		List<BodyAreaDiscomfortObservation> discomfortObservations,
		RecoveryTrainingLoadContextResult trainingLoadContext,
		Instant calculatedAt) {

}
