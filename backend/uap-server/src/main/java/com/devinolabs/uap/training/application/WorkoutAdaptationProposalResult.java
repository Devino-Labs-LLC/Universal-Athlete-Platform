package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentApplicability;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutAdaptationProposalResult(
		WorkoutAdaptationProposalId id,
		AthleteId athleteId,
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		WorkoutOccurrenceId workoutOccurrenceId,
		WorkoutAdaptationProposalOrigin origin,
		RecommendationProvenanceResult recommendationProvenance,
		List<RecommendationAdjustmentResult> recommendationAdjustments,
		WorkoutAdaptationEnvironmentContextResult environmentContext,
		long occurrenceVersionAtGeneration,
		Instant occurrenceUpdatedAtAtGeneration,
		String feasibilityFingerprint,
		WorkoutAdaptationProposalStatus status,
		int totalExecutions,
		int alreadyFeasibleExecutions,
		int proposedSubstitutions,
		int unresolvedExecutions,
		int excludedExecutions,
		int expectedFeasibleExecutions,
		BigDecimal expectedFeasibilityPercentage,
		int expectedFeasibilityIfAllProposedAccepted,
		int acceptedFeasibilityExecutions,
		int unresolvedCount,
		Instant generatedAt,
		Instant expiresAt,
		Instant appliedAt,
		Instant cancelledAt,
		List<WorkoutAdaptationProposalItemResult> items,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public record RecommendationProvenanceResult(
			java.util.UUID recommendationId,
			java.util.UUID readinessAssessmentId,
			java.util.UUID stateSnapshotId,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			TrainingRecommendationAction overallAction,
			ReadinessBand readinessBand) {
	}

	public record RecommendationAdjustmentResult(
			TrainingAdjustmentType type,
			TrainingAdjustmentApplicability applicability,
			List<ReadinessDimensionType> sourceDimensions,
			List<TrainingRecommendationReasonCode> reasonCodes,
			String explanationKey,
			int orderIndex) {
	}

}
