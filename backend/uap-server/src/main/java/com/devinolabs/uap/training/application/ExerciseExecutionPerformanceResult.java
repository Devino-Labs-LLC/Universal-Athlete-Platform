package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetrics;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * One execution's performance, keyed by the stable exercise performance key rather than by name.
 */
public record ExerciseExecutionPerformanceResult(
		WorkoutExerciseExecutionId executionId,
		WorkoutOccurrenceId occurrenceId,
		ExercisePerformanceKey exercisePerformanceKey,
		String exerciseName,
		ExerciseCategory category,
		ExerciseType type,
		int displayOrder,
		WorkoutExerciseExecutionStatus status,
		LocalDate scheduledDate,
		Instant completedAt,
		ExercisePerformanceMetrics metrics) {
}
