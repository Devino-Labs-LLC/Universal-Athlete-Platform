package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.List;

import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;

public record TrainingPlanFeasibilityResult(
		TrainingPlanId trainingPlanId,
		String trainingPlanName,
		WorkoutFeasibilityStatus status,
		int totalExercises,
		int feasibleExercises,
		int infeasibleExercises,
		int exercisesWithoutEnvironmentContext,
		int analyzableExercises,
		BigDecimal feasibilityPercentage,
		List<WorkoutDayFeasibilityResult> daySummaries) {

	public TrainingPlanFeasibilityResult {
		daySummaries = daySummaries == null ? List.of() : List.copyOf(daySummaries);
	}

}
