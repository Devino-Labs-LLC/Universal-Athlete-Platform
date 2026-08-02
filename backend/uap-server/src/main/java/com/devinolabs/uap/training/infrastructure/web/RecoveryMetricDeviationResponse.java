package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.application.RecoveryMetricDeviationResult;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

record RecoveryMetricDeviationResponse(
		RecoveryMetricType metricType,
		String scaleDirection,
		RecoveryMetricValueResponse targetValue,
		RecoveryMetricBaselineResponse baseline,
		BigDecimal absoluteDifference,
		BigDecimal percentageDifference,
		BigDecimal standardizedDeviation,
		RecoveryComparisonBand comparisonBand,
		RecoveryBaselineDataSufficiency dataSufficiency,
		RecoveryAnalyticsReasonCode reasonCode) {

	static RecoveryMetricDeviationResponse from(RecoveryMetricDeviationResult result) {
		return new RecoveryMetricDeviationResponse(
				result.metricType(),
				result.scaleDirection().name(),
				RecoveryMetricValueResponse.of(result.targetValue(), result.metricType()),
				RecoveryMetricBaselineResponse.from(result.baseline()),
				result.absoluteDifference(),
				result.percentageDifference(),
				result.standardizedDeviation(),
				result.comparisonBand(),
				result.dataSufficiency(),
				result.reasonCode());
	}

}
