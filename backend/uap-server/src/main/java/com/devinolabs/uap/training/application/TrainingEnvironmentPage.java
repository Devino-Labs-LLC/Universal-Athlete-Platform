package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.TrainingEnvironment;

public record TrainingEnvironmentPage(
		List<TrainingEnvironment> environments,
		int page,
		int size,
		long totalElements) {
}
