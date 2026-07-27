package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.ExercisePerformanceKey;

public record AthleteExercisePerformanceHistoryResult(
		ExercisePerformanceKey exercisePerformanceKey,
		String exerciseName,
		List<ExerciseExecutionPerformanceResult> entries,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
