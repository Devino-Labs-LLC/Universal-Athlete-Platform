package com.devinolabs.uap.training.application;

import java.util.List;

public record DailyTrainingRecommendationHistoryPage(
		List<DailyTrainingRecommendationSummary> content,
		int page,
		int size,
		long totalElements) {
}
