package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationProposalItemResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;

record WorkoutAdaptationProposalItemResponse(
		UUID id,
		UUID workoutExerciseExecutionId,
		UUID sourceWorkoutExerciseId,
		int executionOrder,
		UUID prescribedExerciseDefinitionId,
		String prescribedNameSnapshot,
		UUID currentPerformedExerciseDefinitionId,
		String currentPerformedNameSnapshot,
		boolean currentFeasible,
		boolean prescribedFeasible,
		boolean performedFeasible,
		List<EquipmentType> missingRequiredEquipment,
		FeasibilityReasonCode analysisReasonCode,
		WorkoutAdaptationAction action,
		UUID generatedTargetExerciseDefinitionId,
		String generatedTargetNameSnapshot,
		UUID generatedRelationshipId,
		ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot,
		ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot,
		String generatedRationaleSnapshot,
		UUID selectedTargetExerciseDefinitionId,
		UUID selectedRelationshipId,
		WorkoutAdaptationDecision athleteDecision,
		String athleteNotes,
		List<WorkoutAdaptationAlternativeResponse> alternatives,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	static WorkoutAdaptationProposalItemResponse from(WorkoutAdaptationProposalItemResult result) {
		return new WorkoutAdaptationProposalItemResponse(
				result.id().value(),
				result.workoutExerciseExecutionId().value(),
				result.sourceWorkoutExerciseId().value(),
				result.executionOrder(),
				result.prescribedExerciseDefinitionId().value(),
				result.prescribedNameSnapshot(),
				result.currentPerformedExerciseDefinitionId().value(),
				result.currentPerformedNameSnapshot(),
				result.currentFeasible(),
				result.prescribedFeasible(),
				result.performedFeasible(),
				result.missingRequiredEquipment(),
				result.analysisReasonCode(),
				result.action(),
				result.generatedTargetExerciseDefinitionId() == null
						? null
						: result.generatedTargetExerciseDefinitionId().value(),
				result.generatedTargetNameSnapshot(),
				result.generatedRelationshipId() == null ? null : result.generatedRelationshipId().value(),
				result.generatedRelationshipTypeSnapshot(),
				result.generatedCompatibilitySnapshot(),
				result.generatedRationaleSnapshot(),
				result.selectedTargetExerciseDefinitionId() == null
						? null
						: result.selectedTargetExerciseDefinitionId().value(),
				result.selectedRelationshipId() == null ? null : result.selectedRelationshipId().value(),
				result.athleteDecision(),
				result.athleteNotes(),
				result.alternatives().stream().map(WorkoutAdaptationAlternativeResponse::from).toList(),
				result.createdAt(),
				result.updatedAt(),
				result.version());
	}

}
