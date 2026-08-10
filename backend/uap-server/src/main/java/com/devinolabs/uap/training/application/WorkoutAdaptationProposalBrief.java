package com.devinolabs.uap.training.application;

import java.util.UUID;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

/**
 * Header-only active adaptation proposal facts for client facades.
 */
public record WorkoutAdaptationProposalBrief(
		UUID proposalId,
		UUID workoutOccurrenceId,
		WorkoutAdaptationProposalStatus status,
		WorkoutAdaptationProposalOrigin origin,
		int unresolvedCount) {
}
