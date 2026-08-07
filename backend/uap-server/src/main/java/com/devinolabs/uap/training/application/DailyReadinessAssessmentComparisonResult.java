package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;

public record DailyReadinessAssessmentComparisonResult(
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
		List<ReadinessDimensionDifferenceResult> dimensionChanges) {
}
