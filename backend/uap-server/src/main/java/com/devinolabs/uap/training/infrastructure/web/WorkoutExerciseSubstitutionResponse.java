package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutExerciseSubstitutionResult;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;

record WorkoutExerciseSubstitutionResponse(
		UUID id,
		UUID workoutOccurrenceId,
		UUID workoutExerciseExecutionId,
		UUID fromExerciseDefinitionId,
		String fromExerciseName,
		UUID toExerciseDefinitionId,
		String toExerciseName,
		ExerciseSubstitutionReason reason,
		String notes,
		boolean reverted,
		Instant changedAt) {

	static WorkoutExerciseSubstitutionResponse from(WorkoutExerciseSubstitutionResult result) {
		return new WorkoutExerciseSubstitutionResponse(
				result.id().value(),
				result.workoutOccurrenceId().value(),
				result.workoutExerciseExecutionId().value(),
				result.fromExerciseDefinitionId().value(),
				result.fromExerciseName(),
				result.toExerciseDefinitionId().value(),
				result.toExerciseName(),
				result.reason(),
				result.notes(),
				result.reverted(),
				result.changedAt());
	}

}
