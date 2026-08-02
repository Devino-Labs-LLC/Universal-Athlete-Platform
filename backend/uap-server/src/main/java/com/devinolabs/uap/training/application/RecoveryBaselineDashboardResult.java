package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecoveryBaselineDashboardResult(
		LocalDate targetDate,
		boolean checkInPresent,
		DailyRecoveryCheckInResult checkIn,
		int baselineWindowDays,
		List<RecoveryMetricBaselineResult> baselines,
		List<RecoveryMetricDeviationResult> metricDeviations,
		List<RecoveryMetricDashboardTrendResult> metricTrends,
		RecoveryTrainingLoadContextResult trainingLoadContext,
		Instant calculatedAt) {

}
