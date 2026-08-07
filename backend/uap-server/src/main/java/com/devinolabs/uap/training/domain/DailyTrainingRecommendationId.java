package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public record DailyTrainingRecommendationId(UUID value) {

	public DailyTrainingRecommendationId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static DailyTrainingRecommendationId generate() {
		return new DailyTrainingRecommendationId(UUID.randomUUID());
	}

	public static DailyTrainingRecommendationId of(UUID value) {
		return new DailyTrainingRecommendationId(value);
	}

}
