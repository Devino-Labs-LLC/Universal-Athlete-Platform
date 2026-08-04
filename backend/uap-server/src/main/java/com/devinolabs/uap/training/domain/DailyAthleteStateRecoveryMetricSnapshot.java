package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record DailyAthleteStateRecoveryMetricSnapshot(
		RecoveryMetricType metricType,
		BigDecimal targetValue,
		RecoveryMetricDirection metricDirection,
		int observationCount,
		RecoveryBaselineDataSufficiency dataSufficiency,
		BigDecimal baselineMean,
		BigDecimal baselineMedian,
		BigDecimal baselineMinimum,
		BigDecimal baselineMaximum,
		BigDecimal baselineStandardDeviation,
		BigDecimal absoluteDifference,
		BigDecimal percentageDifference,
		BigDecimal standardizedDeviation,
		RecoveryComparisonBand comparisonBand,
		RecoveryAnalyticsReasonCode reasonCode) {

	public DailyAthleteStateRecoveryMetricSnapshot {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(metricDirection, "metricDirection must not be null");
		Objects.requireNonNull(dataSufficiency, "dataSufficiency must not be null");
		Objects.requireNonNull(comparisonBand, "comparisonBand must not be null");
	}

}
