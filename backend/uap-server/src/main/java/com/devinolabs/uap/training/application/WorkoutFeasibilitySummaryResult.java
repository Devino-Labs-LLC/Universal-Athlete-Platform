package com.devinolabs.uap.training.application;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;

public record WorkoutFeasibilitySummaryResult(
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

	static WorkoutFeasibilitySummaryResult from(
			com.devinolabs.uap.training.domain.WorkoutFeasibilityStatusResolver.WorkoutFeasibilitySummary summary) {
		return new WorkoutFeasibilitySummaryResult(
				summary.status(),
				summary.totalExercises(),
				summary.feasibleExercises(),
				summary.infeasibleExercises(),
				summary.substitutedExecutions(),
				summary.feasibleAfterExistingSubstitution(),
				summary.exercisesWithCompatibleSuggestions(),
				summary.exercisesWithoutCompatibleSuggestions(),
				summary.feasibilityPercentage(),
				summary.environmentContextPresent());
	}

}
