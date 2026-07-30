package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.application.TrainingEnvironmentResultPage;

public record TrainingEnvironmentPageResponse(
		List<TrainingEnvironmentResponse> environments,
		int page,
		int size,
		long totalElements) {

	static TrainingEnvironmentPageResponse from(TrainingEnvironmentResultPage page) {
		return new TrainingEnvironmentPageResponse(
				page.environments().stream().map(TrainingEnvironmentResponse::from).toList(),
				page.page(),
				page.size(),
				page.totalElements());
	}
}
