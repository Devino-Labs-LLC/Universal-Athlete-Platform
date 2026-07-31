package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationApplicationResult;
import com.devinolabs.uap.training.application.WorkoutAdaptationAppliedItemResult;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

record WorkoutAdaptationAppliedItemResponse(
		UUID executionId,
		UUID proposalItemId,
		UUID fromExerciseDefinitionId,
		UUID toExerciseDefinitionId,
		UUID relationshipId,
		WorkoutAdaptationDecision decision,
		UUID historyId,
		WorkoutAdaptationEnvironmentContextResponse environmentContext) {

	static WorkoutAdaptationAppliedItemResponse from(WorkoutAdaptationAppliedItemResult result) {
		return new WorkoutAdaptationAppliedItemResponse(
				result.executionId().value(),
				result.proposalItemId().value(),
				result.fromExerciseDefinitionId().value(),
				result.toExerciseDefinitionId() == null ? null : result.toExerciseDefinitionId().value(),
				result.relationshipId() == null ? null : result.relationshipId().value(),
				result.decision(),
				result.historyId() == null ? null : result.historyId().value(),
				WorkoutAdaptationEnvironmentContextResponse.from(result.environmentContext()));
	}

}

record WorkoutAdaptationApplicationResponse(
		UUID proposalId,
		WorkoutAdaptationProposalStatus proposalStatus,
		Instant appliedAt,
		int substitutionsApplied,
		int executionsUnchanged,
		int explicitlyExcludedExecutions,
		WorkoutOccurrenceFeasibilityResponse finalWorkoutFeasibility,
		List<WorkoutAdaptationAppliedItemResponse> appliedItems,
		List<WorkoutAdaptationAppliedItemResponse> excludedItems) {

	static WorkoutAdaptationApplicationResponse from(WorkoutAdaptationApplicationResult result) {
		return new WorkoutAdaptationApplicationResponse(
				result.proposalId().value(),
				result.proposalStatus(),
				result.appliedAt(),
				result.substitutionsApplied(),
				result.executionsUnchanged(),
				result.explicitlyExcludedExecutions(),
				WorkoutOccurrenceFeasibilityResponse.from(result.finalWorkoutFeasibility()),
				result.appliedItems().stream().map(WorkoutAdaptationAppliedItemResponse::from).toList(),
				result.excludedItems().stream().map(WorkoutAdaptationAppliedItemResponse::from).toList());
	}

}
