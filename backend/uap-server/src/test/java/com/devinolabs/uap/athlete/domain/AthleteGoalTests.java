package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AthleteGoalTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsValidGoalWithDefaultsAndNormalization() {
		AthleteGoal goal = AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.IMPROVE_STRENGTH,
				null,
				"  Improve   Vertical Jump  ",
				"  Build power  ",
				null,
				GoalTarget.of(new BigDecimal("100.5"), GoalTargetUnit.KILOGRAM),
				LocalDate.of(2026, 12, 1),
				null,
				CLOCK);

		assertThat(goal.title()).isEqualTo("Improve   Vertical Jump");
		assertThat(goal.normalizedTitle()).isEqualTo("improve vertical jump");
		assertThat(goal.description()).isEqualTo("Build power");
		assertThat(goal.priority()).isEqualTo(GoalPriority.MEDIUM);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
		assertThat(goal.completedAt()).isNull();
		assertThat(goal.version()).isZero();
	}

	@Test
	void requiresAndRejectsCustomGoalNameForOtherType() {
		AthleteGoal other = AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.OTHER,
				"  Custom Race  ",
				"Finish race",
				null,
				GoalPriority.HIGH,
				null,
				null,
				null,
				CLOCK);
		assertThat(other.customGoalName()).isEqualTo("Custom Race");

		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.OTHER,
				" ",
				"Finish race",
				null,
				null,
				null,
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.LOSE_WEIGHT,
				"Not allowed",
				"Cut weight",
				null,
				null,
				null,
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsBlankAndOversizedTitlesAndDescriptions() {
		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(), AthleteId.generate(), GoalType.MAINTENANCE, null, "  ", null, null, null, null,
				null, CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(), AthleteId.generate(), GoalType.MAINTENANCE, null, "x".repeat(161), null, null,
				null, null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(), AthleteId.generate(), GoalType.MAINTENANCE, null, "ok", "d".repeat(1001), null,
				null, null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);

		AthleteGoal blankDescription = AthleteGoal.create(
				AthleteGoalId.generate(), AthleteId.generate(), GoalType.MAINTENANCE, null, "ok", "   ", null, null, null,
				null, CLOCK);
		assertThat(blankDescription.description()).isNull();
	}

	@Test
	void enforcesTargetAllOrNonePositiveAndCustomUnitRules() {
		assertThat(GoalTarget.none()).isNull();
		assertThat(GoalTarget.optional(null, null, null)).isNull();

		assertThatThrownBy(() -> GoalTarget.optional(new BigDecimal("10"), null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> GoalTarget.optional(null, GoalTargetUnit.KILOGRAM, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> GoalTarget.of(BigDecimal.ZERO, GoalTargetUnit.KILOGRAM))
				.isInstanceOf(IllegalArgumentException.class);

		GoalTarget other = GoalTarget.of(new BigDecimal("3"), GoalTargetUnit.OTHER, "  laps  ");
		assertThat(other.customUnit()).isEqualTo("laps");
		assertThat(other.value()).isEqualByComparingTo("3.000");

		assertThatThrownBy(() -> GoalTarget.of(new BigDecimal("3"), GoalTargetUnit.OTHER, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> GoalTarget.of(new BigDecimal("3"), GoalTargetUnit.KILOGRAM, "kg"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsTargetDateBeforeCreationDate() {
		assertThatThrownBy(() -> AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.RUN_DISTANCE,
				null,
				"10k",
				null,
				null,
				null,
				LocalDate.of(2026, 7, 23),
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void supportsValidStatusTransitionsAndCompletedAtConsistency() {
		AthleteGoal goal = createActiveGoal();

		goal.pause(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.PAUSED);
		assertThat(goal.completedAt()).isNull();

		goal.resume(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);

		goal.complete(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.COMPLETED);
		assertThat(goal.completedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));

		goal.reopen(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
		assertThat(goal.completedAt()).isNull();

		goal.cancel(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.CANCELLED);
		assertThat(goal.completedAt()).isNull();

		goal.reopen(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
	}

	@Test
	void rejectsInvalidStatusTransitions() {
		AthleteGoal completed = createActiveGoal();
		completed.complete(LATER);

		assertThatThrownBy(() -> completed.pause(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.cancel(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.resume(LATER)).isInstanceOf(IllegalStateException.class);

		AthleteGoal cancelled = createActiveGoal();
		cancelled.cancel(LATER);
		assertThatThrownBy(() -> cancelled.complete(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> cancelled.pause(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> cancelled.resume(LATER)).isInstanceOf(IllegalStateException.class);

		AthleteGoal paused = createActiveGoal();
		paused.pause(LATER);
		assertThatThrownBy(() -> paused.reopen(LATER)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void statusOperationsAreIdempotentWhenAlreadyInTargetState() {
		AthleteGoal goal = createActiveGoal();
		goal.pause(LATER);
		goal.pause(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.PAUSED);

		goal.resume(LATER);
		goal.resume(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);

		goal.complete(LATER);
		goal.complete(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.COMPLETED);

		goal.reopen(LATER);
		goal.cancel(LATER);
		goal.cancel(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.CANCELLED);

		goal.reopen(LATER);
		goal.reopen(LATER);
		assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
	}

	@Test
	void rejectsEditingCompletedOrCancelledGoalsUntilReopened() {
		AthleteGoal completed = createActiveGoal();
		completed.complete(LATER);
		assertThatThrownBy(() -> completed.updateDetails("New", null, GoalPriority.HIGH, LATER))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.updateTarget(null, LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.linkSport(AthleteSportId.generate(), LATER))
				.isInstanceOf(IllegalStateException.class);

		AthleteGoal cancelled = createActiveGoal();
		cancelled.cancel(LATER);
		assertThatThrownBy(() -> cancelled.changePriority(GoalPriority.LOW, LATER))
				.isInstanceOf(IllegalStateException.class);

		completed.reopen(LATER);
		completed.updateDetails("New Title", "desc", GoalPriority.HIGH, LATER);
		AthleteSportId sportId = AthleteSportId.generate();
		completed.linkSport(sportId, LATER);
		assertThat(completed.athleteSportId()).isEqualTo(sportId);
		completed.unlinkSport(LATER);
		assertThat(completed.athleteSportId()).isNull();
		assertThat(completed.title()).isEqualTo("New Title");
	}

	private static AthleteGoal createActiveGoal() {
		return AthleteGoal.create(
				AthleteGoalId.generate(),
				AthleteId.generate(),
				GoalType.IMPROVE_ENDURANCE,
				null,
				"5k pace",
				null,
				GoalPriority.MEDIUM,
				null,
				LocalDate.of(2026, 8, 1),
				null,
				CLOCK);
	}

}
