package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutAdaptationProposalSummaryResult(
		WorkoutAdaptationProposalId id,
		WorkoutOccurrenceId workoutOccurrenceId,
		WorkoutAdaptationProposalStatus status,
		int totalExecutions,
		int expectedFeasibleExecutions,
		BigDecimal expectedFeasibilityPercentage,
		int unresolvedCount,
		Instant generatedAt,
		Instant expiresAt,
		long version) {
}
