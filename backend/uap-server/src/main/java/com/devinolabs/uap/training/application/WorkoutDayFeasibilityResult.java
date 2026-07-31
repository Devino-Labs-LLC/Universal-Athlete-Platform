package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;

public record WorkoutDayFeasibilityResult(
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		String workoutDayTitle,
		FeasibilityEnvironmentContextResult environmentContext,
		WorkoutFeasibilitySummaryResult summary,
		List<ExerciseFeasibilityAnalysisResult> exercises) {

	public WorkoutDayFeasibilityResult {
		exercises = exercises == null ? List.of() : List.copyOf(exercises);
	}

}
