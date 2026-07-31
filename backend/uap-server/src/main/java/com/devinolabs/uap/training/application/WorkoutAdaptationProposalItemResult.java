package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

public record WorkoutAdaptationProposalItemResult(
		WorkoutAdaptationProposalItemId id,
		WorkoutExerciseExecutionId workoutExerciseExecutionId,
		WorkoutExerciseId sourceWorkoutExerciseId,
		int executionOrder,
		ExerciseDefinitionId prescribedExerciseDefinitionId,
		String prescribedNameSnapshot,
		ExerciseDefinitionId currentPerformedExerciseDefinitionId,
		String currentPerformedNameSnapshot,
		boolean currentFeasible,
		boolean prescribedFeasible,
		boolean performedFeasible,
		List<EquipmentType> missingRequiredEquipment,
		FeasibilityReasonCode analysisReasonCode,
		WorkoutAdaptationAction action,
		ExerciseDefinitionId generatedTargetExerciseDefinitionId,
		String generatedTargetNameSnapshot,
		ExerciseSubstitutionRelationshipId generatedRelationshipId,
		ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot,
		ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot,
		String generatedRationaleSnapshot,
		ExerciseDefinitionId selectedTargetExerciseDefinitionId,
		ExerciseSubstitutionRelationshipId selectedRelationshipId,
		WorkoutAdaptationDecision athleteDecision,
		String athleteNotes,
		List<WorkoutAdaptationAlternativeResult> alternatives,
		Instant createdAt,
		Instant updatedAt,
		long version) {
}
