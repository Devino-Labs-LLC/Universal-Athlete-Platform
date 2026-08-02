package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;

public record RecoveryMetricTrendResult(
		RecoveryMetricType metricType,
		LocalDate startDate,
		LocalDate endDate,
		int observationCount,
		RecoveryTrendDirection trendDirection,
		RecoveryAnalyticsReasonCode trendReasonCode,
		List<RecoveryMetricTrendPointResult> points) {

}
