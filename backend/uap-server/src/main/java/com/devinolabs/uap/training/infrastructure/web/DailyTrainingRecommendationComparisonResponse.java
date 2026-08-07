package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyTrainingRecommendationComparisonResult;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;

record DailyTrainingRecommendationComparisonResponse(
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

	static DailyTrainingRecommendationComparisonResponse from(
			DailyTrainingRecommendationComparisonResult result) {
		return new DailyTrainingRecommendationComparisonResponse(
				result.olderRecommendationId(),
				result.newerRecommendationId(),
				result.olderStateDate(),
				result.newerStateDate(),
				result.olderReadinessAssessmentId(),
				result.newerReadinessAssessmentId(),
				result.olderSnapshotId(),
				result.newerSnapshotId(),
				result.olderSnapshotVersion(),
				result.newerSnapshotVersion(),
				result.actionChanged(),
				result.priorAction(),
				result.newAction(),
				result.adjustmentsAdded(),
				result.adjustmentsRemoved(),
				result.limitingDimensionsChanged(),
				result.olderLimitingDimensions(),
				result.newerLimitingDimensions());
	}

}
