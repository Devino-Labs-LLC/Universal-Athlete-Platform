package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of a recommendation adjustment captured on proposal generation.
 */
public record WorkoutAdaptationRecommendationAdjustmentSnapshot(
		UUID id,
		TrainingAdjustmentType trainingAdjustmentType,
		TrainingAdjustmentApplicability applicability,
		List<TrainingRecommendationReasonCode> reasonCodes,
		List<ReadinessDimensionType> sourceDimensions,
		String explanationKey,
		int orderIndex) {

	public WorkoutAdaptationRecommendationAdjustmentSnapshot {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(trainingAdjustmentType, "trainingAdjustmentType must not be null");
		Objects.requireNonNull(applicability, "applicability must not be null");
		Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
		Objects.requireNonNull(sourceDimensions, "sourceDimensions must not be null");
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
		reasonCodes = List.copyOf(reasonCodes);
		sourceDimensions = List.copyOf(sourceDimensions);
	}

	public static WorkoutAdaptationRecommendationAdjustmentSnapshot from(
			TrainingRecommendationAdjustment adjustment) {
		Objects.requireNonNull(adjustment, "adjustment must not be null");
		return new WorkoutAdaptationRecommendationAdjustmentSnapshot(
				UUID.randomUUID(),
				adjustment.type(),
				TrainingAdjustmentApplicabilityResolver.resolve(adjustment.type()),
				adjustment.reasonCodes(),
				adjustment.sourceDimensions(),
				adjustment.explanationKey(),
				adjustment.orderIndex());
	}

}
