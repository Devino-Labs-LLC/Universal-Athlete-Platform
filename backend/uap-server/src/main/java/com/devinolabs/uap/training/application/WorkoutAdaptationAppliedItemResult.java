package com.devinolabs.uap.training.application;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistoryId;

public record WorkoutAdaptationAppliedItemResult(
		WorkoutExerciseExecutionId executionId,
		WorkoutAdaptationProposalItemId proposalItemId,
		ExerciseDefinitionId fromExerciseDefinitionId,
		ExerciseDefinitionId toExerciseDefinitionId,
		ExerciseSubstitutionRelationshipId relationshipId,
		WorkoutAdaptationDecision decision,
		WorkoutExerciseSubstitutionHistoryId historyId,
		WorkoutAdaptationEnvironmentContextResult environmentContext) {
}
