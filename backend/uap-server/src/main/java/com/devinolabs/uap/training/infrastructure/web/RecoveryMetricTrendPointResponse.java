package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.RecoveryMetricTrendPointResult;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

record RecoveryMetricTrendPointResponse(
		LocalDate date,
		UUID checkInId,
		RecoveryMetricValueResponse value,
		BigDecimal rollingAverage3,
		BigDecimal rollingAverage7,
		RecoveryTrainingLoadContextResponse trainingLoadContext) {

	static RecoveryMetricTrendPointResponse from(
			RecoveryMetricTrendPointResult result,
			RecoveryMetricType metricType) {
		return new RecoveryMetricTrendPointResponse(
				result.date(),
				result.checkInId(),
				RecoveryMetricValueResponse.of(result.value(), metricType),
				result.rollingAverage3(),
				result.rollingAverage7(),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoadContext()));
	}

}
