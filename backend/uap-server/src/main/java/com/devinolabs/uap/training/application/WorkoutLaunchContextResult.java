package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

public record WorkoutLaunchContextResult(
		OccurrenceSection occurrence,
		List<ExerciseSection> exercises,
		EnvironmentSection environment,
		FeasibilitySection feasibility,
		RecommendationContextSection recommendationContext,
		AdaptationSection adaptation,
		ActionsSection actions) {

	public record OccurrenceSection(
			UUID occurrenceId,
			UUID trainingPlanId,
			UUID workoutDayId,
			WorkoutOccurrenceStatus status,
			LocalDate scheduledDate,
			Instant startedAt,
			Instant completedAt,
			boolean startEligible) {
	}

	public record ExerciseSection(
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
	}

	public record EnvironmentSection(
			UUID plannedEnvironmentId,
			String plannedEnvironmentName,
			List<EquipmentType> plannedEquipment,
			UUID actualEnvironmentId,
			String actualEnvironmentName,
			List<EquipmentType> actualEquipment,
			List<EquipmentType> availableEquipment) {
	}

	public record FeasibilitySection(
			boolean feasibilityPresent,
			WorkoutFeasibilityStatus status,
			int totalExercises,
			int feasibleExercises,
			int infeasibleExercises,
			BigDecimal feasibilityPercentage,
			int exercisesWithCompatibleSuggestions,
			int exercisesWithoutCompatibleSuggestions) {
	}

	public record RecommendationContextSection(
			boolean recommendationPresent,
			UUID recommendationId,
			TrainingRecommendationAction overallAction,
			ReadinessBand readinessBand,
			List<TrainingAdjustmentType> adjustmentTypes,
			boolean occurrenceInRecommendationContexts) {
	}

	public record AdaptationSection(
			boolean activeProposalPresent,
			UUID adaptationProposalId,
			WorkoutAdaptationProposalStatus status,
			int unresolvedCount) {
	}

	public record ActionsSection(
			TrainingClientActionFlag canStart,
			TrainingClientActionFlag canChangeEnvironment,
			TrainingClientActionFlag canGenerateAdaptation,
			TrainingClientActionFlag canApplyAdaptation,
			TrainingClientActionFlag canSubstituteExercise) {
	}

}
