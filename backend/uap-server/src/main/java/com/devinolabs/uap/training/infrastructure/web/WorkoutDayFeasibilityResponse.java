package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutDayFeasibilityResult;

record WorkoutDayFeasibilityResponse(
		UUID trainingPlanId,
		UUID workoutDayId,
		String workoutDayTitle,
		FeasibilityEnvironmentContextResponse environmentContext,
		WorkoutFeasibilitySummaryResponse summary,
		List<ExerciseFeasibilityAnalysisResponse> exercises) {

	static WorkoutDayFeasibilityResponse from(WorkoutDayFeasibilityResult result) {
		return new WorkoutDayFeasibilityResponse(
				result.trainingPlanId().value(),
				result.workoutDayId().value(),
				result.workoutDayTitle(),
				FeasibilityEnvironmentContextResponse.from(result.environmentContext()),
				WorkoutFeasibilitySummaryResponse.from(result.summary()),
				ExerciseFeasibilityAnalysisResponse.fromList(result.exercises()));
	}

}
