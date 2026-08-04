package com.devinolabs.uap.training.application;

import java.util.List;

public record DailyAthleteStateHistoryPage(
		List<DailyAthleteStateSnapshotSummary> content,
		int page,
		int size,
		long totalElements) {
}
