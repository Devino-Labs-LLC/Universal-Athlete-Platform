package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricDeviationCalculator;
import com.devinolabs.uap.training.domain.RecoveryMetricDirection;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

public record RecoveryMetricDeviationResult(
		RecoveryMetricType metricType,
		BigDecimal targetValue,
		RecoveryMetricBaselineResult baseline,
		BigDecimal absoluteDifference,
		BigDecimal percentageDifference,
		BigDecimal standardizedDeviation,
		RecoveryComparisonBand comparisonBand,
		RecoveryMetricDirection scaleDirection,
		RecoveryBaselineDataSufficiency dataSufficiency,
		RecoveryAnalyticsReasonCode reasonCode) {

	public static RecoveryMetricDeviationResult from(
			RecoveryMetricDeviationCalculator.RecoveryMetricDeviationResult deviation,
			RecoveryMetricBaselineResult baseline) {
		return new RecoveryMetricDeviationResult(
				deviation.metricType(),
				deviation.targetValue(),
				baseline,
				deviation.absoluteDifference(),
				deviation.percentageDifference(),
				deviation.standardizedDeviation(),
				deviation.comparisonBand(),
				deviation.scaleDirection(),
				deviation.dataSufficiency(),
				deviation.reasonCode());
	}

}
