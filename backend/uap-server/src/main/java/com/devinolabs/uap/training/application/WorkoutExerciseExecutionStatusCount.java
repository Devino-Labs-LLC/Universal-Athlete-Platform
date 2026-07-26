package com.devinolabs.uap.training.application;

import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutExerciseExecutionStatusCount(
		WorkoutOccurrenceId occurrenceId,
		WorkoutExerciseExecutionStatus status,
		long count) {
}
