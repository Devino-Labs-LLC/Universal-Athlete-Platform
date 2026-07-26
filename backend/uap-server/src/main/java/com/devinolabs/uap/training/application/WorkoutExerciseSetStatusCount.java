package com.devinolabs.uap.training.application;

import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;

public record WorkoutExerciseSetStatusCount(
		WorkoutExerciseExecutionId executionId,
		WorkoutExerciseSetStatus status,
		long count) {
}
