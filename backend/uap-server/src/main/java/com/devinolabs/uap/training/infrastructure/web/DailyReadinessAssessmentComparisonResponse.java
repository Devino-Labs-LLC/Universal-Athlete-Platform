package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.CompareDailyReadinessAssessmentsUseCase;
import com.devinolabs.uap.training.application.DailyReadinessAssessmentComparisonResult;
import com.devinolabs.uap.training.application.ReadinessDimensionDifferenceResult;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

record DailyReadinessAssessmentComparisonResponse(
		UUID olderAssessmentId,
		UUID newerAssessmentId,
		LocalDate olderStateDate,
		LocalDate newerStateDate,
		UUID olderSnapshotId,
		UUID newerSnapshotId,
		int olderSnapshotVersion,
		int newerSnapshotVersion,
		BigDecimal olderScore,
		BigDecimal newerScore,
		BigDecimal scoreDelta,
		CompareDailyReadinessAssessmentsUseCase.ScoreDirection scoreDirection,
		boolean bandChanged,
		ReadinessBand olderBand,
		ReadinessBand newerBand,
		boolean dataSufficiencyChanged,
		ReadinessDataSufficiency olderDataSufficiency,
		ReadinessDataSufficiency newerDataSufficiency,
		boolean limitingDimensionsChanged,
		List<ReadinessDimensionType> olderLimitingDimensions,
		List<ReadinessDimensionType> newerLimitingDimensions,
		List<ReadinessDimensionDifferenceResponse> dimensionChanges) {

	static DailyReadinessAssessmentComparisonResponse from(DailyReadinessAssessmentComparisonResult result) {
		return new DailyReadinessAssessmentComparisonResponse(
				result.olderAssessmentId(),
				result.newerAssessmentId(),
				result.olderStateDate(),
				result.newerStateDate(),
				result.olderSnapshotId(),
				result.newerSnapshotId(),
				result.olderSnapshotVersion(),
				result.newerSnapshotVersion(),
				result.olderScore(),
				result.newerScore(),
				result.scoreDelta(),
				result.scoreDirection(),
				result.bandChanged(),
				result.olderBand(),
				result.newerBand(),
				result.dataSufficiencyChanged(),
				result.olderDataSufficiency(),
				result.newerDataSufficiency(),
				result.limitingDimensionsChanged(),
				result.olderLimitingDimensions(),
				result.newerLimitingDimensions(),
				result.dimensionChanges().stream().map(ReadinessDimensionDifferenceResponse::from).toList());
	}

}

record ReadinessDimensionDifferenceResponse(
		ReadinessDimensionType dimensionType,
		BigDecimal olderNormalizedScore,
		BigDecimal newerNormalizedScore,
		ReadinessReasonCode olderReasonCode,
		ReadinessReasonCode newerReasonCode,
		RecoveryComparisonBand olderComparisonBand,
		RecoveryComparisonBand newerComparisonBand) {

	static ReadinessDimensionDifferenceResponse from(ReadinessDimensionDifferenceResult result) {
		return new ReadinessDimensionDifferenceResponse(
				result.dimensionType(),
				result.olderNormalizedScore(),
				result.newerNormalizedScore(),
				result.olderReasonCode(),
				result.newerReasonCode(),
				result.olderComparisonBand(),
				result.newerComparisonBand());
	}

}
