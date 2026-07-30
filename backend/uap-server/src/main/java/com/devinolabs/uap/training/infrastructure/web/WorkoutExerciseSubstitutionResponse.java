package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.EquipmentType;

import com.devinolabs.uap.training.application.WorkoutExerciseSubstitutionResult;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

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
		UUID substitutionRelationshipId,
		ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
		ExerciseSubstitutionCompatibility compatibilitySnapshot,
		UUID trainingEnvironmentId,
		String trainingEnvironmentNameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot,
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
				result.substitutionRelationshipId() == null
						? null
						: result.substitutionRelationshipId().value(),
				result.relationshipTypeSnapshot(),
				result.compatibilitySnapshot(),
				result.trainingEnvironmentId() == null ? null : result.trainingEnvironmentId().value(),
				result.trainingEnvironmentNameSnapshot(),
				result.availableEquipmentSnapshot(),
				result.reverted(),
				result.changedAt());
	}

}
