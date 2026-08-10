package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

class TrainingClientActionResolverTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-31T15:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

	@Test
	void enablesCreateCheckInWhenMissingAndDateValid() {
		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				false,
				false,
				false,
				false,
				null,
				null,
				null,
				false);

		assertThat(actions.canCreateRecoveryCheckIn().allowed()).isTrue();
		assertThat(actions.canUpdateRecoveryCheckIn().allowed()).isFalse();
		assertThat(actions.canUpdateRecoveryCheckIn().reasonCode()).isEqualTo("DAILY_RECOVERY_CHECK_IN_NOT_FOUND");
		assertThat(actions.canGenerateAthleteStateSnapshot().allowed()).isTrue();
		assertThat(actions.canGenerateReadinessAssessment().allowed()).isFalse();
		assertThat(actions.canGenerateReadinessAssessment().reasonCode())
				.isEqualTo("DAILY_ATHLETE_STATE_SNAPSHOT_REQUIRED");
	}

	@Test
	void disablesGenerateSnapshotWhenPresentAndUnlocksReadiness() {
		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				false,
				false,
				null,
				null,
				null,
				false);

		assertThat(actions.canCreateRecoveryCheckIn().allowed()).isFalse();
		assertThat(actions.canCreateRecoveryCheckIn().reasonCode()).isEqualTo("RECOVERY_CHECK_IN_ALREADY_EXISTS");
		assertThat(actions.canUpdateRecoveryCheckIn().allowed()).isTrue();
		assertThat(actions.canGenerateAthleteStateSnapshot().allowed()).isFalse();
		assertThat(actions.canGenerateAthleteStateSnapshot().reasonCode())
				.isEqualTo("DAILY_ATHLETE_STATE_SNAPSHOT_ALREADY_EXISTS");
		assertThat(actions.canGenerateReadinessAssessment().allowed()).isTrue();
		assertThat(actions.canGenerateTrainingRecommendation().allowed()).isFalse();
		assertThat(actions.canGenerateTrainingRecommendation().reasonCode())
				.isEqualTo("DAILY_READINESS_ASSESSMENT_REQUIRED");
	}

	@Test
	void enablesStartForScheduledPrimaryOccurrence() {
		WorkoutOccurrence occurrence = scheduledOccurrence(false);

		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				true,
				true,
				TrainingRecommendationAction.PROCEED_AS_PLANNED,
				occurrence,
				null,
				false);

		assertThat(actions.canStartWorkout().allowed()).isTrue();
		assertThat(actions.canContinueWorkout().allowed()).isFalse();
		assertThat(actions.canSubmitSessionEffort().allowed()).isFalse();
		assertThat(actions.canGenerateAdaptationProposal().allowed()).isFalse();
		assertThat(actions.canGenerateAdaptationProposal().reasonCode())
				.isEqualTo("TRAINING_RECOMMENDATION_NOT_ADAPTATION_ELIGIBLE");
	}

	@Test
	void enablesAdaptationGenerationWhenModifySessionAndEnvironmentPresent() {
		WorkoutOccurrence occurrence = scheduledOccurrence(true);

		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				true,
				true,
				TrainingRecommendationAction.MODIFY_SESSION,
				occurrence,
				null,
				false);

		assertThat(actions.canGenerateAdaptationProposal().allowed()).isTrue();
	}

	@Test
	void enablesContinueForInProgressOccurrence() {
		WorkoutOccurrence occurrence = scheduledOccurrence(true);
		occurrence.start(CLOCK);

		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				true,
				true,
				TrainingRecommendationAction.MODIFY_SESSION,
				occurrence,
				null,
				false);

		assertThat(actions.canStartWorkout().allowed()).isFalse();
		assertThat(actions.canContinueWorkout().allowed()).isTrue();
	}

	@Test
	void enablesSubmitEffortOnlyForCompletedWithoutExistingEffort() {
		WorkoutOccurrence occurrence = scheduledOccurrence(true);
		occurrence.start(CLOCK);
		occurrence.complete(CLOCK);

		TrainingTodayDashboardActionsResult withEffort = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				true,
				true,
				null,
				occurrence,
				null,
				true);
		TrainingTodayDashboardActionsResult withoutEffort = TrainingClientActionResolver.resolveTodayActions(
				TODAY,
				CLOCK,
				true,
				true,
				true,
				true,
				null,
				occurrence,
				null,
				false);

		assertThat(withEffort.canSubmitSessionEffort().allowed()).isFalse();
		assertThat(withEffort.canSubmitSessionEffort().reasonCode())
				.isEqualTo("WORKOUT_SESSION_EFFORT_ALREADY_EXISTS");
		assertThat(withoutEffort.canSubmitSessionEffort().allowed()).isTrue();
	}

	@Test
	void resolveApplyAdaptationDisabledWhenMissing() {
		TrainingClientActionFlag flag = TrainingClientActionResolver.resolveApplyAdaptation(
				(WorkoutAdaptationProposalBrief) null);
		assertThat(flag.allowed()).isFalse();
		assertThat(flag.reasonCode()).isEqualTo("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND");
	}

	private static WorkoutOccurrence scheduledOccurrence(boolean withEnvironment) {
		WorkoutOccurrence occurrence = WorkoutOccurrence.createManual(
				WorkoutOccurrenceId.generate(),
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				TODAY,
				null,
				null,
				CLOCK);
		if (withEnvironment) {
			occurrence.setActualEnvironment(
					WorkoutOccurrenceEnvironmentSnapshot.of(
							TrainingEnvironmentId.generate(),
							"Home Gym",
							List.of(EquipmentType.BARBELL)),
					CLOCK);
		}
		return occurrence;
	}

}
