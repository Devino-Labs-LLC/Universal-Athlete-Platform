package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationProposalResult;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

record WorkoutAdaptationProposalResponse(
		UUID id,
		UUID athleteId,
		UUID trainingPlanId,
		UUID workoutDayId,
		UUID workoutOccurrenceId,
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

}
