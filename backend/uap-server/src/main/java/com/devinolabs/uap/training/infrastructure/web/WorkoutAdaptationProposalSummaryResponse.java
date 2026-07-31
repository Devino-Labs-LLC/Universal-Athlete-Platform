package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationProposalSummaryResult;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

record WorkoutAdaptationProposalSummaryResponse(
		UUID id,
		UUID workoutOccurrenceId,
		WorkoutAdaptationProposalStatus status,
		int totalExecutions,
		int expectedFeasibleExecutions,
		BigDecimal expectedFeasibilityPercentage,
		int unresolvedCount,
		Instant generatedAt,
		Instant expiresAt,
		long version) {

	static WorkoutAdaptationProposalSummaryResponse from(WorkoutAdaptationProposalSummaryResult result) {
		return new WorkoutAdaptationProposalSummaryResponse(
				result.id().value(),
				result.workoutOccurrenceId().value(),
				result.status(),
				result.totalExecutions(),
				result.expectedFeasibleExecutions(),
				result.expectedFeasibilityPercentage(),
				result.unresolvedCount(),
				result.generatedAt(),
				result.expiresAt(),
				result.version());
	}

}
