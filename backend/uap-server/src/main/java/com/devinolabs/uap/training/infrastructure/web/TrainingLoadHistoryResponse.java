package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.application.TrainingLoadHistoryResult;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;

record TrainingLoadHistoryResponse(
		TrainingLoadGranularity granularity,
		List<OccurrenceTrainingLoadHistoryItemResponse> occurrences,
		List<DailyTrainingLoadSummaryResponse> dailySummaries,
		List<WeeklyTrainingLoadSummaryResponse> weeklySummaries,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static TrainingLoadHistoryResponse from(TrainingLoadHistoryResult result) {
		return new TrainingLoadHistoryResponse(
				result.granularity(),
				result.occurrences().stream().map(OccurrenceTrainingLoadHistoryItemResponse::from).toList(),
				result.dailySummaries().stream().map(DailyTrainingLoadSummaryResponse::from).toList(),
				result.weeklySummaries().stream().map(WeeklyTrainingLoadSummaryResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}

}
