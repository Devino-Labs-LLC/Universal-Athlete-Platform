package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutOccurrenceFeasibilityResult;
import com.devinolabs.uap.training.application.WorkoutOccurrenceFeasibilityResult.ExerciseExecutionFeasibilityAnalysisResult;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;

record WorkoutOccurrenceFeasibilityResponse(
		UUID trainingPlanId,
		UUID workoutDayId,
		UUID occurrenceId,
		FeasibilityEnvironmentContextResponse environmentContext,
		WorkoutFeasibilitySummaryResponse summary,
		List<ExerciseExecutionFeasibilityAnalysisResponse> executions) {

	static WorkoutOccurrenceFeasibilityResponse from(WorkoutOccurrenceFeasibilityResult result) {
		return new WorkoutOccurrenceFeasibilityResponse(
				result.trainingPlanId().value(),
				result.workoutDayId().value(),
				result.occurrenceId().value(),
				FeasibilityEnvironmentContextResponse.from(result.environmentContext()),
				WorkoutFeasibilitySummaryResponse.from(result.summary()),
				result.executions().stream().map(ExerciseExecutionFeasibilityAnalysisResponse::from).toList());
	}

	record ExerciseExecutionFeasibilityAnalysisResponse(
			UUID executionId,
			int orderIndex,
			UUID prescribedExerciseDefinitionId,
			String prescribedExerciseName,
			UUID performedExerciseDefinitionId,
			String performedExerciseName,
			boolean substituted,
			ExerciseCompatibilityDetailResponse prescribedCompatibility,
			ExerciseCompatibilityDetailResponse performedCompatibility,
			boolean currentExecutionFeasible,
			ExerciseFeasibilityStatus currentStatus,
			FeasibilityReasonCode reasonCode,
			String reasonSummary,
			int compatibleSubstitutionCount,
			List<ExerciseSubstitutionSuggestionResponse> suggestedSubstitutions,
			boolean hasCompatibleSubstitution) {

		static ExerciseExecutionFeasibilityAnalysisResponse from(ExerciseExecutionFeasibilityAnalysisResult result) {
			return new ExerciseExecutionFeasibilityAnalysisResponse(
					result.executionId().value(),
					result.orderIndex(),
					result.prescribedExerciseDefinitionId().value(),
					result.prescribedExerciseName(),
					result.performedExerciseDefinitionId().value(),
					result.performedExerciseName(),
					result.substituted(),
					ExerciseCompatibilityDetailResponse.from(result.prescribedCompatibility()),
					ExerciseCompatibilityDetailResponse.from(result.performedCompatibility()),
					result.currentExecutionFeasible(),
					result.currentStatus(),
					result.reasonCode(),
					result.reasonSummary(),
					result.compatibleSubstitutionCount(),
					ExerciseSubstitutionSuggestionResponse.fromList(result.suggestedSubstitutions()),
					result.hasCompatibleSubstitution());
		}

	}

}
