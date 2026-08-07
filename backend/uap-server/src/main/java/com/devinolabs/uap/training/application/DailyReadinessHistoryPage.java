package com.devinolabs.uap.training.application;

import java.util.List;

public record DailyReadinessHistoryPage(
		List<DailyReadinessAssessmentSummary> content,
		int page,
		int size,
		long totalElements) {
}
