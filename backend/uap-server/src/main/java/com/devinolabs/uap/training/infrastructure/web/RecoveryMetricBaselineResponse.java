package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.training.application.RecoveryMetricBaselineResult;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

record RecoveryMetricBaselineResponse(
		RecoveryMetricType metricType,
		String scaleDirection,
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

	static RecoveryMetricBaselineResponse from(RecoveryMetricBaselineResult result) {
		return new RecoveryMetricBaselineResponse(
				result.metricType(),
				result.metricType().scaleDirection().name(),
				result.windowDays(),
				result.windowStartDate(),
				result.windowEndDate(),
				result.observationCount(),
				result.dataSufficiency(),
				result.mean(),
				result.median(),
				result.minimum(),
				result.maximum(),
				result.standardDeviation(),
				result.firstObservationDate(),
				result.lastObservationDate(),
				result.calculatedAt());
	}

}
