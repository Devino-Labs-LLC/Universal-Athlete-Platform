package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.application.DailyAthleteStateHistoryPage;

record DailyAthleteStateHistoryResponse(
		List<DailyAthleteStateSnapshotVersionResponse> content,
		int page,
		int size,
		long totalElements) {

	static DailyAthleteStateHistoryResponse from(DailyAthleteStateHistoryPage page) {
		return new DailyAthleteStateHistoryResponse(
				page.content().stream().map(DailyAthleteStateSnapshotVersionResponse::from).toList(),
				page.page(),
				page.size(),
				page.totalElements());
	}

}
