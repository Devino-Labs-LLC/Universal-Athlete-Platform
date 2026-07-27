package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseExecutionPerformanceResult;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;

record ExerciseExecutionPerformanceResponse(
		UUID executionId,
		UUID occurrenceId,
		UUID exercisePerformanceKey,
		String exerciseName,
		ExerciseCategory category,
		ExerciseType type,
		int displayOrder,
		WorkoutExerciseExecutionStatus status,
		LocalDate scheduledDate,
		Instant completedAt,
		ExercisePerformanceMetricsResponse metrics) {

	static ExerciseExecutionPerformanceResponse from(ExerciseExecutionPerformanceResult result) {
		return new ExerciseExecutionPerformanceResponse(
				result.executionId().value(),
				result.occurrenceId().value(),
				result.exercisePerformanceKey().value(),
				result.exerciseName(),
				result.category(),
				result.type(),
				result.displayOrder(),
				result.status(),
				result.scheduledDate(),
				result.completedAt(),
				ExercisePerformanceMetricsResponse.from(result.metrics()));
	}

}
