package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

/**
 * Header-only outstanding adaptation proposal facts for client facades.
 */
public record WorkoutAdaptationProposalOutstandingBrief(
		UUID proposalId,
		UUID workoutOccurrenceId,
		WorkoutAdaptationProposalStatus status,
		int unresolvedCount,
		Instant generatedAt,
		Instant expiresAt) {
}
