package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.application.RecoveryMetricTrendResult;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;

record RecoveryMetricTrendResponse(
		RecoveryMetricType metricType,
		String scaleDirection,
		LocalDate startDate,
		LocalDate endDate,
		int observationCount,
		RecoveryTrendDirection trendDirection,
		RecoveryAnalyticsReasonCode trendReasonCode,
		List<RecoveryMetricTrendPointResponse> points) {

	static RecoveryMetricTrendResponse from(RecoveryMetricTrendResult result) {
		return new RecoveryMetricTrendResponse(
				result.metricType(),
				result.metricType().scaleDirection().name(),
				result.startDate(),
				result.endDate(),
				result.observationCount(),
				result.trendDirection(),
				result.trendReasonCode(),
				result.points().stream()
						.map(point -> RecoveryMetricTrendPointResponse.from(point, result.metricType()))
						.toList());
	}

}
