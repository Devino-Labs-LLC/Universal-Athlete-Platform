package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.AthleteExercisePerformanceHistoryResult;

record AthleteExercisePerformanceHistoryResponse(
		UUID exercisePerformanceKey,
		UUID exerciseDefinitionId,
		String exerciseName,
		List<ExerciseExecutionPerformanceResponse> entries,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static AthleteExercisePerformanceHistoryResponse from(AthleteExercisePerformanceHistoryResult result) {
		return new AthleteExercisePerformanceHistoryResponse(
				result.exercisePerformanceKey().value(),
				result.exercisePerformanceKey().toDefinitionId().value(),
				result.exerciseName(),
				result.entries().stream().map(ExerciseExecutionPerformanceResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}

}
