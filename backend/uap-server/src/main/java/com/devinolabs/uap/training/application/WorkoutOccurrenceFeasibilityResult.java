package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutOccurrenceFeasibilityResult(
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		WorkoutOccurrenceId occurrenceId,
		FeasibilityEnvironmentContextResult environmentContext,
		WorkoutFeasibilitySummaryResult summary,
		List<ExerciseExecutionFeasibilityAnalysisResult> executions) {

	public WorkoutOccurrenceFeasibilityResult {
		executions = executions == null ? List.of() : List.copyOf(executions);
	}

	public record ExerciseExecutionFeasibilityAnalysisResult(
			WorkoutExerciseExecutionId executionId,
			int orderIndex,
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			String prescribedExerciseName,
			ExerciseDefinitionId performedExerciseDefinitionId,
			String performedExerciseName,
			boolean substituted,
			ExerciseCompatibilityDetailResult prescribedCompatibility,
			ExerciseCompatibilityDetailResult performedCompatibility,
			boolean currentExecutionFeasible,
			ExerciseFeasibilityStatus currentStatus,
			FeasibilityReasonCode reasonCode,
			String reasonSummary,
			int compatibleSubstitutionCount,
			List<ExerciseSubstitutionSuggestionResult> suggestedSubstitutions,
			boolean hasCompatibleSubstitution) {

		public ExerciseExecutionFeasibilityAnalysisResult {
			prescribedCompatibility = prescribedCompatibility == null
					? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
					: prescribedCompatibility;
			performedCompatibility = performedCompatibility == null
					? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
					: performedCompatibility;
			suggestedSubstitutions = suggestedSubstitutions == null ? List.of() : List.copyOf(suggestedSubstitutions);
		}

	}

}
