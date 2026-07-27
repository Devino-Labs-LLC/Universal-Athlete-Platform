package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.application.ExerciseDefinitionPageResult;

record ExerciseDefinitionPageResponse(
		List<ExerciseDefinitionResponse> definitions,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static ExerciseDefinitionPageResponse from(ExerciseDefinitionPageResult result) {
		return new ExerciseDefinitionPageResponse(
				result.definitions().stream().map(ExerciseDefinitionResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}

}
