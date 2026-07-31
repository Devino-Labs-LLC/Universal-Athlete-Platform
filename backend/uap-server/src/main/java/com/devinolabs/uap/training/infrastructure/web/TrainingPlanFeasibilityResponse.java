package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.TrainingPlanFeasibilityResult;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;

record TrainingPlanFeasibilityResponse(
		UUID trainingPlanId,
		String trainingPlanName,
		WorkoutFeasibilityStatus status,
		int totalExercises,
		int feasibleExercises,
		int infeasibleExercises,
		int exercisesWithoutEnvironmentContext,
		int analyzableExercises,
		BigDecimal feasibilityPercentage,
		List<WorkoutDayFeasibilityResponse> daySummaries) {

	static TrainingPlanFeasibilityResponse from(TrainingPlanFeasibilityResult result) {
		return new TrainingPlanFeasibilityResponse(
				result.trainingPlanId().value(),
				result.trainingPlanName(),
				result.status(),
				result.totalExercises(),
				result.feasibleExercises(),
				result.infeasibleExercises(),
				result.exercisesWithoutEnvironmentContext(),
				result.analyzableExercises(),
				result.feasibilityPercentage(),
				result.daySummaries().stream().map(WorkoutDayFeasibilityResponse::from).toList());
	}

}
