package com.devinolabs.uap.training.application;

import java.util.List;

public record WorkoutOccurrenceDetailResult(
		WorkoutOccurrenceResult occurrence,
		List<WorkoutExerciseExecutionResult> executions) {
}
