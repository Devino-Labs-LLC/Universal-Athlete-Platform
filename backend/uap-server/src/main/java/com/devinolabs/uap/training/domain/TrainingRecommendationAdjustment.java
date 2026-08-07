package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One categorical adjustment suggested by a daily training recommendation.
 */
public record TrainingRecommendationAdjustment(
		UUID id,
		TrainingAdjustmentType type,
		int priority,
		List<TrainingRecommendationReasonCode> reasonCodes,
		List<ReadinessDimensionType> sourceDimensions,
		String explanationKey,
		int orderIndex) {

	public TrainingRecommendationAdjustment {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
		Objects.requireNonNull(sourceDimensions, "sourceDimensions must not be null");
		Objects.requireNonNull(explanationKey, "explanationKey must not be null");
		if (priority < 1) {
			throw new IllegalArgumentException("priority must be >= 1");
		}
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
		if (explanationKey.isBlank()) {
			throw new IllegalArgumentException("explanationKey must not be blank");
		}
		reasonCodes = List.copyOf(reasonCodes);
		sourceDimensions = List.copyOf(sourceDimensions);
	}

}
