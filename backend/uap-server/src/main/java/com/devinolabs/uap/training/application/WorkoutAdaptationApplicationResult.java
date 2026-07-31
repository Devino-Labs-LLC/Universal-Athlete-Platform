package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistoryId;

public record WorkoutAdaptationApplicationResult(
		WorkoutAdaptationProposalId proposalId,
		WorkoutAdaptationProposalStatus proposalStatus,
		Instant appliedAt,
		int substitutionsApplied,
		int executionsUnchanged,
		int explicitlyExcludedExecutions,
		WorkoutOccurrenceFeasibilityResult finalWorkoutFeasibility,
		List<WorkoutAdaptationAppliedItemResult> appliedItems,
		List<WorkoutAdaptationAppliedItemResult> excludedItems) {
}
