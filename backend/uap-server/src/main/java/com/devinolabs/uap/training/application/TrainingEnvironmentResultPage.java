package com.devinolabs.uap.training.application;

import java.util.List;

public record TrainingEnvironmentResultPage(
		List<TrainingEnvironmentResult> environments,
		int page,
		int size,
		long totalElements) {
}
