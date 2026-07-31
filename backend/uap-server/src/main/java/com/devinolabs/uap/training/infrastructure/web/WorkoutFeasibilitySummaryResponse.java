package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.application.WorkoutFeasibilitySummaryResult;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;

record WorkoutFeasibilitySummaryResponse(
		WorkoutFeasibilityStatus status,
		int totalExercises,
		int feasibleExercises,
		int infeasibleExercises,
		int substitutedExecutions,
		int feasibleAfterExistingSubstitution,
		int exercisesWithCompatibleSuggestions,
		int exercisesWithoutCompatibleSuggestions,
		BigDecimal feasibilityPercentage,
		boolean environmentContextPresent) {

	static WorkoutFeasibilitySummaryResponse from(WorkoutFeasibilitySummaryResult result) {
		return new WorkoutFeasibilitySummaryResponse(
				result.status(),
				result.totalExercises(),
				result.feasibleExercises(),
				result.infeasibleExercises(),
				result.substitutedExecutions(),
				result.feasibleAfterExistingSubstitution(),
				result.exercisesWithCompatibleSuggestions(),
				result.exercisesWithoutCompatibleSuggestions(),
				result.feasibilityPercentage(),
				result.environmentContextPresent());
	}

}
