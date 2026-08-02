package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.application.RecoveryBaselineDashboardResult;
import com.devinolabs.uap.training.application.RecoveryMetricDashboardTrendResult;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;

record RecoveryMetricDashboardTrendResponse(
		RecoveryMetricType metricType,
		String scaleDirection,
		RecoveryTrendDirection trendDirection,
		int observationCount) {

	static RecoveryMetricDashboardTrendResponse from(RecoveryMetricDashboardTrendResult result) {
		return new RecoveryMetricDashboardTrendResponse(
				result.metricType(),
				result.metricType().scaleDirection().name(),
				result.trendDirection(),
				result.observationCount());
	}

}

record RecoveryBaselineDashboardResponse(
		LocalDate targetDate,
		boolean checkInPresent,
		DailyRecoveryCheckInResponse checkIn,
		int baselineWindowDays,
		List<RecoveryMetricBaselineResponse> baselines,
		List<RecoveryMetricDeviationResponse> metricDeviations,
		List<RecoveryMetricDashboardTrendResponse> metricTrends,
		RecoveryTrainingLoadContextResponse trainingLoadContext,
		Instant calculatedAt) {

	static RecoveryBaselineDashboardResponse from(RecoveryBaselineDashboardResult result) {
		return new RecoveryBaselineDashboardResponse(
				result.targetDate(),
				result.checkInPresent(),
				result.checkIn() == null ? null : DailyRecoveryCheckInResponse.from(result.checkIn()),
				result.baselineWindowDays(),
				result.baselines().stream().map(RecoveryMetricBaselineResponse::from).toList(),
				result.metricDeviations().stream().map(RecoveryMetricDeviationResponse::from).toList(),
				result.metricTrends().stream().map(RecoveryMetricDashboardTrendResponse::from).toList(),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoadContext()),
				result.calculatedAt());
	}

}
