package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

public record ExerciseFeasibilityAnalysisResult(
		WorkoutExerciseId workoutExerciseId,
		ExerciseDefinitionId exerciseDefinitionId,
		String name,
		int orderIndex,
		boolean feasible,
		ExerciseCompatibilityDetailResult compatibility,
		ExerciseFeasibilityStatus currentStatus,
		FeasibilityReasonCode reasonCode,
		String reasonSummary,
		int compatibleSubstitutionCount,
		List<ExerciseSubstitutionSuggestionResult> suggestedSubstitutions,
		boolean hasCompatibleSubstitution) {

	public ExerciseFeasibilityAnalysisResult {
		compatibility = compatibility == null
				? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
				: compatibility;
		suggestedSubstitutions = suggestedSubstitutions == null ? List.of() : List.copyOf(suggestedSubstitutions);
	}

}
