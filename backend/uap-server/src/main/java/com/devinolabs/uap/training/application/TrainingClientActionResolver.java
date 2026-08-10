package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import com.devinolabs.uap.training.domain.InvalidRecoveryCheckInDateException;
import com.devinolabs.uap.training.domain.RecoveryCheckInDateOutOfRangeException;
import com.devinolabs.uap.training.domain.RecoveryCheckInDateValidator;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Derives client action flags from existing domain status facts.
 * Convenience only — server write endpoints remain authoritative.
 */
public final class TrainingClientActionResolver {

	private TrainingClientActionResolver() {
	}

	public static TrainingTodayDashboardActionsResult resolveTodayActions(
			LocalDate date,
			Clock clock,
			boolean checkInPresent,
			boolean snapshotPresent,
			boolean readinessPresent,
			boolean recommendationPresent,
			TrainingRecommendationAction recommendationAction,
			WorkoutOccurrence primaryOccurrence,
			WorkoutAdaptationProposalBrief activeProposal,
			boolean sessionEffortPresent) {
		Objects.requireNonNull(date, "date must not be null");
		Objects.requireNonNull(clock, "clock must not be null");

		TrainingClientActionFlag createCheckIn = resolveCreateCheckIn(date, clock, checkInPresent);
		TrainingClientActionFlag updateCheckIn = checkInPresent
				? resolveUpdateCheckIn(date, clock)
				: TrainingClientActionFlag.disabled("DAILY_RECOVERY_CHECK_IN_NOT_FOUND");
		TrainingClientActionFlag generateSnapshot = snapshotPresent
				? TrainingClientActionFlag.disabled("DAILY_ATHLETE_STATE_SNAPSHOT_ALREADY_EXISTS")
				: TrainingClientActionFlag.enabled();
		TrainingClientActionFlag generateReadiness;
		if (!snapshotPresent) {
			generateReadiness = TrainingClientActionFlag.disabled("DAILY_ATHLETE_STATE_SNAPSHOT_REQUIRED");
		}
		else if (readinessPresent) {
			generateReadiness = TrainingClientActionFlag.disabled("DAILY_READINESS_ASSESSMENT_ALREADY_EXISTS");
		}
		else {
			generateReadiness = TrainingClientActionFlag.enabled();
		}
		TrainingClientActionFlag generateRecommendation;
		if (!readinessPresent) {
			generateRecommendation = TrainingClientActionFlag.disabled("DAILY_READINESS_ASSESSMENT_REQUIRED");
		}
		else if (recommendationPresent) {
			generateRecommendation = TrainingClientActionFlag.disabled("DAILY_TRAINING_RECOMMENDATION_ALREADY_EXISTS");
		}
		else {
			generateRecommendation = TrainingClientActionFlag.enabled();
		}

		boolean occurrenceModifiable = primaryOccurrence != null
				&& (primaryOccurrence.status() == WorkoutOccurrenceStatus.SCHEDULED
						|| primaryOccurrence.status() == WorkoutOccurrenceStatus.IN_PROGRESS);
		boolean environmentPresent = primaryOccurrence != null
				&& primaryOccurrence.actualEnvironment() != null;
		TrainingClientActionFlag generateAdaptation;
		if (recommendationAction != TrainingRecommendationAction.MODIFY_SESSION) {
			generateAdaptation = TrainingClientActionFlag.disabled("TRAINING_RECOMMENDATION_NOT_ADAPTATION_ELIGIBLE");
		}
		else if (!occurrenceModifiable) {
			generateAdaptation = TrainingClientActionFlag.disabled("RECOMMENDED_ADAPTATION_OCCURRENCE_NOT_ELIGIBLE");
		}
		else if (!environmentPresent) {
			generateAdaptation = TrainingClientActionFlag.disabled("WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET");
		}
		else if (activeProposal != null && activeProposal.status() != null && activeProposal.status().active()) {
			generateAdaptation = TrainingClientActionFlag.disabled("ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS");
		}
		else {
			generateAdaptation = TrainingClientActionFlag.enabled();
		}

		TrainingClientActionFlag startWorkout = primaryOccurrence != null
				&& primaryOccurrence.status() == WorkoutOccurrenceStatus.SCHEDULED
				? TrainingClientActionFlag.enabled()
				: TrainingClientActionFlag.disabled("INVALID_WORKOUT_OCCURRENCE_STATUS");
		TrainingClientActionFlag continueWorkout = primaryOccurrence != null
				&& primaryOccurrence.status() == WorkoutOccurrenceStatus.IN_PROGRESS
				? TrainingClientActionFlag.enabled()
				: TrainingClientActionFlag.disabled("INVALID_WORKOUT_OCCURRENCE_STATUS");
		TrainingClientActionFlag submitEffort;
		if (primaryOccurrence == null
				|| primaryOccurrence.status() != WorkoutOccurrenceStatus.COMPLETED) {
			submitEffort = TrainingClientActionFlag.disabled("INVALID_WORKOUT_OCCURRENCE_STATUS");
		}
		else if (sessionEffortPresent) {
			submitEffort = TrainingClientActionFlag.disabled("WORKOUT_SESSION_EFFORT_ALREADY_EXISTS");
		}
		else {
			submitEffort = TrainingClientActionFlag.enabled();
		}

		return new TrainingTodayDashboardActionsResult(
				createCheckIn,
				updateCheckIn,
				generateSnapshot,
				generateReadiness,
				generateRecommendation,
				generateAdaptation,
				startWorkout,
				continueWorkout,
				submitEffort);
	}

	public static TrainingClientActionFlag resolveApplyAdaptation(WorkoutAdaptationProposal proposal) {
		if (proposal == null) {
			return TrainingClientActionFlag.disabled("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND");
		}
		return resolveApplyAdaptation(new WorkoutAdaptationProposalBrief(
				proposal.id().value(),
				proposal.workoutOccurrenceId().value(),
				proposal.status(),
				proposal.origin(),
				proposal.unresolvedCount()));
	}

	public static TrainingClientActionFlag resolveApplyAdaptation(WorkoutAdaptationProposalBrief proposal) {
		if (proposal == null) {
			return TrainingClientActionFlag.disabled("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND");
		}
		if (proposal.status() == WorkoutAdaptationProposalStatus.EXPIRED) {
			return TrainingClientActionFlag.disabled("WORKOUT_ADAPTATION_PROPOSAL_EXPIRED");
		}
		if (proposal.status().terminal()) {
			return TrainingClientActionFlag.disabled("WORKOUT_ADAPTATION_PROPOSAL_TERMINAL");
		}
		if (proposal.unresolvedCount() > 0) {
			return TrainingClientActionFlag.disabled("WORKOUT_ADAPTATION_PROPOSAL_UNRESOLVED");
		}
		return TrainingClientActionFlag.enabled();
	}

	private static TrainingClientActionFlag resolveCreateCheckIn(LocalDate date, Clock clock, boolean present) {
		if (present) {
			return TrainingClientActionFlag.disabled("RECOVERY_CHECK_IN_ALREADY_EXISTS");
		}
		try {
			RecoveryCheckInDateValidator.validate(date, clock);
			return TrainingClientActionFlag.enabled();
		}
		catch (InvalidRecoveryCheckInDateException ex) {
			return TrainingClientActionFlag.disabled("INVALID_RECOVERY_CHECK_IN_DATE");
		}
		catch (RecoveryCheckInDateOutOfRangeException ex) {
			return TrainingClientActionFlag.disabled("RECOVERY_CHECK_IN_DATE_OUT_OF_RANGE");
		}
	}

	private static TrainingClientActionFlag resolveUpdateCheckIn(LocalDate date, Clock clock) {
		try {
			RecoveryCheckInDateValidator.validate(date, clock);
			return TrainingClientActionFlag.enabled();
		}
		catch (InvalidRecoveryCheckInDateException ex) {
			return TrainingClientActionFlag.disabled("INVALID_RECOVERY_CHECK_IN_DATE");
		}
		catch (RecoveryCheckInDateOutOfRangeException ex) {
			return TrainingClientActionFlag.disabled("RECOVERY_CHECK_IN_DATE_OUT_OF_RANGE");
		}
	}

}
