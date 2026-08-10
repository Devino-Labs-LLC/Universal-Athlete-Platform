package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutLaunchContextResult;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

record WorkoutLaunchContextResponse(
		WorkoutLaunchOccurrenceResponse occurrence,
		List<WorkoutLaunchExerciseResponse> exercises,
		WorkoutLaunchEnvironmentResponse environment,
		WorkoutLaunchFeasibilityResponse feasibility,
		WorkoutLaunchRecommendationContextResponse recommendationContext,
		WorkoutLaunchAdaptationResponse adaptation,
		WorkoutLaunchActionsResponse actions) {

	static WorkoutLaunchContextResponse from(WorkoutLaunchContextResult result) {
		return new WorkoutLaunchContextResponse(
				WorkoutLaunchOccurrenceResponse.from(result.occurrence()),
				result.exercises().stream().map(WorkoutLaunchExerciseResponse::from).toList(),
				WorkoutLaunchEnvironmentResponse.from(result.environment()),
				WorkoutLaunchFeasibilityResponse.from(result.feasibility()),
				WorkoutLaunchRecommendationContextResponse.from(result.recommendationContext()),
				WorkoutLaunchAdaptationResponse.from(result.adaptation()),
				WorkoutLaunchActionsResponse.from(result.actions()));
	}

}

record WorkoutLaunchOccurrenceResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		WorkoutOccurrenceStatus status,
		LocalDate scheduledDate,
		Instant startedAt,
		Instant completedAt,
		boolean startEligible) {

	static WorkoutLaunchOccurrenceResponse from(WorkoutLaunchContextResult.OccurrenceSection section) {
		return new WorkoutLaunchOccurrenceResponse(
				section.occurrenceId(),
				section.trainingPlanId(),
				section.workoutDayId(),
				section.status(),
				section.scheduledDate(),
				section.startedAt(),
				section.completedAt(),
				section.startEligible());
	}
}

record WorkoutLaunchExerciseResponse(
		UUID executionId,
		int orderIndex,
		UUID prescribedExerciseDefinitionId,
		String prescribedExerciseName,
		UUID performedExerciseDefinitionId,
		String performedExerciseName,
		boolean substituted,
		ExerciseSubstitutionReason substitutionReason,
		WorkoutExerciseExecutionStatus status,
		Integer prescribedSets,
		Integer prescribedMinimumReps,
		Integer prescribedMaximumReps,
		BigDecimal prescribedTargetWeight,
		WeightUnit prescribedWeightUnit,
		Integer prescribedTargetDurationSeconds,
		BigDecimal prescribedTargetDistance,
		DistanceUnit prescribedDistanceUnit,
		Integer prescribedTargetRestSeconds,
		Integer prescribedTargetRpe) {

	static WorkoutLaunchExerciseResponse from(WorkoutLaunchContextResult.ExerciseSection section) {
		return new WorkoutLaunchExerciseResponse(
				section.executionId(),
				section.orderIndex(),
				section.prescribedExerciseDefinitionId(),
				section.prescribedExerciseName(),
				section.performedExerciseDefinitionId(),
				section.performedExerciseName(),
				section.substituted(),
				section.substitutionReason(),
				section.status(),
				section.prescribedSets(),
				section.prescribedMinimumReps(),
				section.prescribedMaximumReps(),
				section.prescribedTargetWeight(),
				section.prescribedWeightUnit(),
				section.prescribedTargetDurationSeconds(),
				section.prescribedTargetDistance(),
				section.prescribedDistanceUnit(),
				section.prescribedTargetRestSeconds(),
				section.prescribedTargetRpe());
	}
}

record WorkoutLaunchEnvironmentResponse(
		UUID plannedEnvironmentId,
		String plannedEnvironmentName,
		List<EquipmentType> plannedEquipment,
		UUID actualEnvironmentId,
		String actualEnvironmentName,
		List<EquipmentType> actualEquipment,
		List<EquipmentType> availableEquipment) {

	static WorkoutLaunchEnvironmentResponse from(WorkoutLaunchContextResult.EnvironmentSection section) {
		return new WorkoutLaunchEnvironmentResponse(
				section.plannedEnvironmentId(),
				section.plannedEnvironmentName(),
				section.plannedEquipment(),
				section.actualEnvironmentId(),
				section.actualEnvironmentName(),
				section.actualEquipment(),
				section.availableEquipment());
	}
}

record WorkoutLaunchFeasibilityResponse(
		boolean feasibilityPresent,
		WorkoutFeasibilityStatus status,
		int totalExercises,
		int feasibleExercises,
		int infeasibleExercises,
		BigDecimal feasibilityPercentage,
		int exercisesWithCompatibleSuggestions,
		int exercisesWithoutCompatibleSuggestions) {

	static WorkoutLaunchFeasibilityResponse from(WorkoutLaunchContextResult.FeasibilitySection section) {
		return new WorkoutLaunchFeasibilityResponse(
				section.feasibilityPresent(),
				section.status(),
				section.totalExercises(),
				section.feasibleExercises(),
				section.infeasibleExercises(),
				section.feasibilityPercentage(),
				section.exercisesWithCompatibleSuggestions(),
				section.exercisesWithoutCompatibleSuggestions());
	}
}

record WorkoutLaunchRecommendationContextResponse(
		boolean recommendationPresent,
		UUID recommendationId,
		TrainingRecommendationAction overallAction,
		ReadinessBand readinessBand,
		List<TrainingAdjustmentType> adjustmentTypes,
		boolean occurrenceInRecommendationContexts) {

	static WorkoutLaunchRecommendationContextResponse from(
			WorkoutLaunchContextResult.RecommendationContextSection section) {
		return new WorkoutLaunchRecommendationContextResponse(
				section.recommendationPresent(),
				section.recommendationId(),
				section.overallAction(),
				section.readinessBand(),
				section.adjustmentTypes(),
				section.occurrenceInRecommendationContexts());
	}
}

record WorkoutLaunchAdaptationResponse(
		boolean activeProposalPresent,
		UUID adaptationProposalId,
		WorkoutAdaptationProposalStatus status,
		int unresolvedCount) {

	static WorkoutLaunchAdaptationResponse from(WorkoutLaunchContextResult.AdaptationSection section) {
		return new WorkoutLaunchAdaptationResponse(
				section.activeProposalPresent(),
				section.adaptationProposalId(),
				section.status(),
				section.unresolvedCount());
	}
}

record WorkoutLaunchActionsResponse(
		TrainingClientActionFlagResponse canStart,
		TrainingClientActionFlagResponse canChangeEnvironment,
		TrainingClientActionFlagResponse canGenerateAdaptation,
		TrainingClientActionFlagResponse canApplyAdaptation,
		TrainingClientActionFlagResponse canSubstituteExercise) {

	static WorkoutLaunchActionsResponse from(WorkoutLaunchContextResult.ActionsSection section) {
		return new WorkoutLaunchActionsResponse(
				TrainingClientActionFlagResponse.from(section.canStart()),
				TrainingClientActionFlagResponse.from(section.canChangeEnvironment()),
				TrainingClientActionFlagResponse.from(section.canGenerateAdaptation()),
				TrainingClientActionFlagResponse.from(section.canApplyAdaptation()),
				TrainingClientActionFlagResponse.from(section.canSubstituteExercise()));
	}
}
