package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutAdaptationProposalResult(
		WorkoutAdaptationProposalId id,
		AthleteId athleteId,
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		WorkoutOccurrenceId workoutOccurrenceId,
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
}
