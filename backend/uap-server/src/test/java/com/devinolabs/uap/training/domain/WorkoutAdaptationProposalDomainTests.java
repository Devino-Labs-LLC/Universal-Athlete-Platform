package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutAdaptationProposalDomainTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void statusResolverMarksReadyWhenAllSubstitutesResolved() {
		WorkoutAdaptationProposalItem feasible = item(WorkoutAdaptationAction.NO_CHANGE, WorkoutAdaptationDecision.NOT_REQUIRED);
		WorkoutAdaptationProposalItem accepted = item(WorkoutAdaptationAction.SUBSTITUTE, WorkoutAdaptationDecision.ACCEPTED);
		WorkoutAdaptationProposalItem rejected = item(WorkoutAdaptationAction.EXCLUDED, WorkoutAdaptationDecision.REJECTED);

		assertThat(WorkoutAdaptationProposalStatusResolver.resolve(List.of(feasible, accepted, rejected)))
				.isEqualTo(WorkoutAdaptationProposalStatus.READY);
	}

	@Test
	void statusResolverMarksPartiallyResolvedWhenPendingOrUnresolvedRemain() {
		WorkoutAdaptationProposalItem pending = item(WorkoutAdaptationAction.SUBSTITUTE, WorkoutAdaptationDecision.PENDING);
		WorkoutAdaptationProposalItem unresolved = item(WorkoutAdaptationAction.UNRESOLVED, WorkoutAdaptationDecision.PENDING);

		assertThat(WorkoutAdaptationProposalStatusResolver.resolve(List.of(pending)))
				.isEqualTo(WorkoutAdaptationProposalStatus.PARTIALLY_RESOLVED);
		assertThat(WorkoutAdaptationProposalStatusResolver.resolve(List.of(unresolved)))
				.isEqualTo(WorkoutAdaptationProposalStatus.PARTIALLY_RESOLVED);
	}

	@Test
	void fingerprintIsStableForIdenticalInput() {
		WorkoutAdaptationFeasibilityFingerprint.FingerprintInput input = new WorkoutAdaptationFeasibilityFingerprint.FingerprintInput(
				WorkoutOccurrenceId.generate(),
				3L,
				WorkoutOccurrenceStatus.SCHEDULED,
				FeasibilityEnvironmentContextSource.OCCURRENCE_ACTUAL_SNAPSHOT,
				TrainingEnvironmentId.generate(),
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				List.of(new WorkoutAdaptationFeasibilityFingerprint.FingerprintExecution(
						WorkoutExerciseExecutionId.generate(),
						1L,
						SystemExerciseDefinitions.BENCH_PRESS,
						WorkoutExerciseExecutionStatus.NOT_STARTED,
						List.of(new WorkoutAdaptationFeasibilityFingerprint.FingerprintSet(
								WorkoutExerciseSetId.generate(),
								WorkoutExerciseSetStatus.NOT_STARTED)))),
				java.util.Map.of(),
				java.util.Map.of());

		String first = WorkoutAdaptationFeasibilityFingerprint.compute(input).value();
		String second = WorkoutAdaptationFeasibilityFingerprint.compute(input).value();

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSize(64);
	}

	@Test
	void expectedFeasibilityCountsPendingSubstitutesButNotRejectedItems() {
		WorkoutAdaptationProposal proposal = proposalWithItems(
				itemWithAction(WorkoutAdaptationAction.NO_CHANGE, WorkoutAdaptationDecision.NOT_REQUIRED, true),
				itemWithAction(WorkoutAdaptationAction.SUBSTITUTE, WorkoutAdaptationDecision.PENDING, false),
				itemWithAction(WorkoutAdaptationAction.EXCLUDED, WorkoutAdaptationDecision.REJECTED, false));

		assertThat(proposal.totalExecutions()).isEqualTo(3);
		assertThat(proposal.alreadyFeasibleExecutions()).isEqualTo(1);
		assertThat(proposal.proposedSubstitutions()).isEqualTo(1);
		assertThat(proposal.excludedExecutions()).isEqualTo(1);
		assertThat(proposal.expectedFeasibleExecutions()).isEqualTo(2);
		assertThat(proposal.expectedFeasibilityIfAllProposedAccepted()).isEqualTo(2);
		assertThat(proposal.expectedFeasibilityPercentage()).isEqualByComparingTo(new BigDecimal("66.67"));
	}

	private static WorkoutAdaptationProposalItem item(
			WorkoutAdaptationAction action,
			WorkoutAdaptationDecision decision) {
		return WorkoutAdaptationProposalItem.rehydrate(
				WorkoutAdaptationProposalItemId.generate(),
				WorkoutAdaptationProposalId.generate(),
				WorkoutExerciseExecutionId.generate(),
				WorkoutExerciseId.generate(),
				1,
				SystemExerciseDefinitions.BENCH_PRESS,
				"Bench Press",
				SystemExerciseDefinitions.BENCH_PRESS,
				"Bench Press",
				ExercisePerformanceKey.of(SystemExerciseDefinitions.BENCH_PRESS),
				action == WorkoutAdaptationAction.NO_CHANGE,
				true,
				false,
				List.of(EquipmentType.BARBELL),
				FeasibilityReasonCode.MISSING_REQUIRED_EQUIPMENT,
				action,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				"Goblet Squat",
				null,
				null,
				null,
				null,
				action == WorkoutAdaptationAction.SUBSTITUTE ? SystemExerciseDefinitions.GOBLET_SQUAT : null,
				null,
				decision,
				null,
				List.of(),
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				0L);
	}

	private static WorkoutAdaptationProposalItem itemWithAction(
			WorkoutAdaptationAction action,
			WorkoutAdaptationDecision decision,
			boolean currentFeasible) {
		return WorkoutAdaptationProposalItem.rehydrate(
				WorkoutAdaptationProposalItemId.generate(),
				WorkoutAdaptationProposalId.generate(),
				WorkoutExerciseExecutionId.generate(),
				WorkoutExerciseId.generate(),
				1,
				SystemExerciseDefinitions.PLANK,
				"Plank",
				SystemExerciseDefinitions.BENCH_PRESS,
				"Bench Press",
				ExercisePerformanceKey.of(SystemExerciseDefinitions.BENCH_PRESS),
				currentFeasible,
				true,
				false,
				List.of(),
				FeasibilityReasonCode.ALL_REQUIRED_EQUIPMENT_AVAILABLE,
				action,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				"Goblet Squat",
				null,
				null,
				null,
				null,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				null,
				decision,
				null,
				List.of(),
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				0L);
	}

	private static WorkoutAdaptationProposal proposalWithItems(WorkoutAdaptationProposalItem... items) {
		WorkoutAdaptationProposal proposal = WorkoutAdaptationProposal.generate(
				WorkoutAdaptationProposalId.generate(),
				AthleteId.of(UUID.randomUUID()),
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				WorkoutOccurrence.rehydrate(
						WorkoutOccurrenceId.generate(),
						TrainingPlanId.generate(),
						WorkoutDayId.generate(),
						AthleteId.of(UUID.randomUUID()),
						java.time.LocalDate.of(2026, 6, 8),
						null,
						null,
						null,
						WorkoutOccurrenceStatus.SCHEDULED,
						null,
						WorkoutOccurrenceOrigin.MANUAL,
						null,
						null,
						false,
						null,
						null,
						null,
						Instant.now(CLOCK),
						Instant.now(CLOCK),
						0L),
				FeasibilityEnvironmentContextSource.OCCURRENCE_ACTUAL_SNAPSHOT,
				TrainingEnvironmentId.generate(),
				"Home Gym",
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				WorkoutAdaptationFeasibilityFingerprint.of("a".repeat(64)),
				List.of(items),
				30,
				CLOCK);
		proposal.refreshSummary();
		proposal.refreshStatus();
		return proposal;
	}

}
