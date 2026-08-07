package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationProposalResult;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentApplicability;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

record WorkoutAdaptationProposalResponse(
		UUID id,
		UUID athleteId,
		UUID trainingPlanId,
		UUID workoutDayId,
		UUID workoutOccurrenceId,
		WorkoutAdaptationProposalOrigin origin,
		RecommendationProvenanceResponse recommendationProvenance,
		List<RecommendationAdjustmentResponse> recommendationAdjustments,
		WorkoutAdaptationEnvironmentContextResponse environmentContext,
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
		List<WorkoutAdaptationProposalItemResponse> items,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	static WorkoutAdaptationProposalResponse from(WorkoutAdaptationProposalResult result) {
		return new WorkoutAdaptationProposalResponse(
				result.id().value(),
				result.athleteId().value(),
				result.trainingPlanId().value(),
				result.workoutDayId().value(),
				result.workoutOccurrenceId().value(),
				result.origin(),
				result.recommendationProvenance() == null
						? null
						: RecommendationProvenanceResponse.from(result.recommendationProvenance()),
				result.recommendationAdjustments().stream()
						.map(RecommendationAdjustmentResponse::from)
						.toList(),
				WorkoutAdaptationEnvironmentContextResponse.from(result.environmentContext()),
				result.occurrenceVersionAtGeneration(),
				result.occurrenceUpdatedAtAtGeneration(),
				result.feasibilityFingerprint(),
				result.status(),
				result.totalExecutions(),
				result.alreadyFeasibleExecutions(),
				result.proposedSubstitutions(),
				result.unresolvedExecutions(),
				result.excludedExecutions(),
				result.expectedFeasibleExecutions(),
				result.expectedFeasibilityPercentage(),
				result.expectedFeasibilityIfAllProposedAccepted(),
				result.acceptedFeasibilityExecutions(),
				result.unresolvedCount(),
				result.generatedAt(),
				result.expiresAt(),
				result.appliedAt(),
				result.cancelledAt(),
				result.items().stream().map(WorkoutAdaptationProposalItemResponse::from).toList(),
				result.createdAt(),
				result.updatedAt(),
				result.version());
	}

	record RecommendationProvenanceResponse(
			UUID recommendationId,
			UUID readinessAssessmentId,
			UUID stateSnapshotId,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			TrainingRecommendationAction overallAction,
			ReadinessBand readinessBand) {

		static RecommendationProvenanceResponse from(
				WorkoutAdaptationProposalResult.RecommendationProvenanceResult provenance) {
			return new RecommendationProvenanceResponse(
					provenance.recommendationId(),
					provenance.readinessAssessmentId(),
					provenance.stateSnapshotId(),
					provenance.recommendationAlgorithmVersion(),
					provenance.overallAction(),
					provenance.readinessBand());
		}
	}

	record RecommendationAdjustmentResponse(
			TrainingAdjustmentType type,
			TrainingAdjustmentApplicability applicability,
			List<ReadinessDimensionType> sourceDimensions,
			List<TrainingRecommendationReasonCode> reasonCodes,
			String explanationKey,
			int orderIndex) {

		static RecommendationAdjustmentResponse from(
				WorkoutAdaptationProposalResult.RecommendationAdjustmentResult adjustment) {
			return new RecommendationAdjustmentResponse(
					adjustment.type(),
					adjustment.applicability(),
					adjustment.sourceDimensions(),
					adjustment.reasonCodes(),
					adjustment.explanationKey(),
					adjustment.orderIndex());
		}
	}

}
