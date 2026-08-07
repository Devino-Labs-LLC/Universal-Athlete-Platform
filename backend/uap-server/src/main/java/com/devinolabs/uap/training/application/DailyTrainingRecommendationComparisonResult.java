package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;

public record DailyTrainingRecommendationComparisonResult(
		UUID olderRecommendationId,
		UUID newerRecommendationId,
		LocalDate olderStateDate,
		LocalDate newerStateDate,
		UUID olderReadinessAssessmentId,
		UUID newerReadinessAssessmentId,
		UUID olderSnapshotId,
		UUID newerSnapshotId,
		int olderSnapshotVersion,
		int newerSnapshotVersion,
		boolean actionChanged,
		TrainingRecommendationAction priorAction,
		TrainingRecommendationAction newAction,
		List<TrainingAdjustmentType> adjustmentsAdded,
		List<TrainingAdjustmentType> adjustmentsRemoved,
		boolean limitingDimensionsChanged,
		List<ReadinessDimensionType> olderLimitingDimensions,
		List<ReadinessDimensionType> newerLimitingDimensions) {
}
