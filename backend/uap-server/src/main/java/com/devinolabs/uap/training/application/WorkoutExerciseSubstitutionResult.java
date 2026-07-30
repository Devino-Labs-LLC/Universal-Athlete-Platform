package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistoryId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutExerciseSubstitutionResult(
		WorkoutExerciseSubstitutionHistoryId id,
		WorkoutOccurrenceId workoutOccurrenceId,
		WorkoutExerciseExecutionId workoutExerciseExecutionId,
		ExerciseDefinitionId fromExerciseDefinitionId,
		String fromExerciseName,
		ExerciseDefinitionId toExerciseDefinitionId,
		String toExerciseName,
		ExerciseSubstitutionReason reason,
		String notes,
		ExerciseSubstitutionRelationshipId substitutionRelationshipId,
		ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
		ExerciseSubstitutionCompatibility compatibilitySnapshot,
		TrainingEnvironmentId trainingEnvironmentId,
		String trainingEnvironmentNameSnapshot,
		List<EquipmentType> availableEquipmentSnapshot,
		boolean reverted,
		Instant changedAt) {
}
