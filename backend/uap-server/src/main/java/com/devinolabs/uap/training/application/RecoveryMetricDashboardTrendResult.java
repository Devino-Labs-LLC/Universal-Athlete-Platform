package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;

public record RecoveryMetricDashboardTrendResult(
		RecoveryMetricType metricType,
		RecoveryTrendDirection trendDirection,
		int observationCount) {

}
