package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;

public record TrainingLoadHistoryResult(
		TrainingLoadGranularity granularity,
		List<WorkoutOccurrenceLoadSummaryResult> occurrences,
		List<DailyTrainingLoadSummary> dailySummaries,
		List<WeeklyTrainingLoadSummary> weeklySummaries,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
