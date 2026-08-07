package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

public record ReadinessDimensionDifferenceResult(
		ReadinessDimensionType dimensionType,
		BigDecimal olderNormalizedScore,
		BigDecimal newerNormalizedScore,
		ReadinessReasonCode olderReasonCode,
		ReadinessReasonCode newerReasonCode,
		RecoveryComparisonBand olderComparisonBand,
		RecoveryComparisonBand newerComparisonBand) {
}
