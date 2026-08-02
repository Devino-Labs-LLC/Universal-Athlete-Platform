package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryMetricBaseline;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

public record RecoveryMetricBaselineResult(
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

	public static RecoveryMetricBaselineResult from(RecoveryMetricBaseline baseline) {
		return new RecoveryMetricBaselineResult(
				baseline.metricType(),
				baseline.windowDays(),
				baseline.windowStartDate(),
				baseline.windowEndDate(),
				baseline.observationCount(),
				baseline.dataSufficiency(),
				baseline.mean(),
				baseline.median(),
				baseline.minimum(),
				baseline.maximum(),
				baseline.standardDeviation(),
				baseline.firstObservationDate(),
				baseline.lastObservationDate(),
				baseline.calculatedAt());
	}

}
