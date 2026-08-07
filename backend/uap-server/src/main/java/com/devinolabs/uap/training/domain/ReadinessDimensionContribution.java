package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record ReadinessDimensionContribution(
		ReadinessDimensionType dimensionType,
		RecoveryMetricType sourceMetricType,
		boolean available,
		RecoveryBaselineDataSufficiency baselineSufficiency,
		BigDecimal targetValue,
		BigDecimal baselineMean,
		BigDecimal standardizedDeviation,
		RecoveryComparisonBand comparisonBand,
		BigDecimal normalizedScore,
		BigDecimal configuredWeight,
		BigDecimal effectiveWeight,
		BigDecimal weightedContribution,
		ReadinessReasonCode reasonCode,
		Integer rankAsLimiting,
		Integer rankAsStrongest) {

	public ReadinessDimensionContribution {
		Objects.requireNonNull(dimensionType, "dimensionType must not be null");
		Objects.requireNonNull(configuredWeight, "configuredWeight must not be null");
		Objects.requireNonNull(reasonCode, "reasonCode must not be null");
	}

	public ReadinessDimensionContribution withRanks(Integer limiting, Integer strongest) {
		return new ReadinessDimensionContribution(
				dimensionType,
				sourceMetricType,
				available,
				baselineSufficiency,
				targetValue,
				baselineMean,
				standardizedDeviation,
				comparisonBand,
				normalizedScore,
				configuredWeight,
				effectiveWeight,
				weightedContribution,
				reasonCode,
				limiting,
				strongest);
	}

	public ReadinessDimensionContribution withEffectiveWeight(
			BigDecimal effectiveWeight,
			BigDecimal weightedContribution) {
		return new ReadinessDimensionContribution(
				dimensionType,
				sourceMetricType,
				available,
				baselineSufficiency,
				targetValue,
				baselineMean,
				standardizedDeviation,
				comparisonBand,
				normalizedScore,
				configuredWeight,
				effectiveWeight,
				weightedContribution,
				reasonCode,
				rankAsLimiting,
				rankAsStrongest);
	}

}
