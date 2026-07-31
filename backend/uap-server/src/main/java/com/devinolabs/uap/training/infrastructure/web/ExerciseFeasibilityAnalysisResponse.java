package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseFeasibilityAnalysisResult;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;

record ExerciseFeasibilityAnalysisResponse(
		UUID workoutExerciseId,
		UUID exerciseDefinitionId,
		String name,
		int orderIndex,
		boolean feasible,
		ExerciseCompatibilityDetailResponse compatibility,
		ExerciseFeasibilityStatus currentStatus,
		FeasibilityReasonCode reasonCode,
		String reasonSummary,
		int compatibleSubstitutionCount,
		List<ExerciseSubstitutionSuggestionResponse> suggestedSubstitutions,
		boolean hasCompatibleSubstitution) {

	static ExerciseFeasibilityAnalysisResponse from(ExerciseFeasibilityAnalysisResult result) {
		return new ExerciseFeasibilityAnalysisResponse(
				result.workoutExerciseId().value(),
				result.exerciseDefinitionId().value(),
				result.name(),
				result.orderIndex(),
				result.feasible(),
				ExerciseCompatibilityDetailResponse.from(result.compatibility()),
				result.currentStatus(),
				result.reasonCode(),
				result.reasonSummary(),
				result.compatibleSubstitutionCount(),
				ExerciseSubstitutionSuggestionResponse.fromList(result.suggestedSubstitutions()),
				result.hasCompatibleSubstitution());
	}

	static List<ExerciseFeasibilityAnalysisResponse> fromList(List<ExerciseFeasibilityAnalysisResult> results) {
		return results.stream().map(ExerciseFeasibilityAnalysisResponse::from).toList();
	}

}
