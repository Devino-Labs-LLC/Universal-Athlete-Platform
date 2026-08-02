package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Athlete-specific prior-only baseline statistics for one recovery metric.
 */
public record RecoveryMetricBaseline(
		RecoveryMetricType metricType,
		int windowDays,
		LocalDate windowStartDate,
		LocalDate windowEndDate,
		int observationCount,
		RecoveryBaselineDataSufficiency dataSufficiency,
		BigDecimal mean,
		BigDecimal median,
		BigDecimal minimum,
		BigDecimal maximum,
		BigDecimal standardDeviation,
		LocalDate firstObservationDate,
		LocalDate lastObservationDate,
		Instant calculatedAt) {

	public RecoveryMetricBaseline {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(dataSufficiency, "dataSufficiency must not be null");
		Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
	}

}
