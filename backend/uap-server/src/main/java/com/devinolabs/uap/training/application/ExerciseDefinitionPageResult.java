package com.devinolabs.uap.training.application;

import java.util.List;

public record ExerciseDefinitionPageResult(
		List<ExerciseDefinitionResult> definitions,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
