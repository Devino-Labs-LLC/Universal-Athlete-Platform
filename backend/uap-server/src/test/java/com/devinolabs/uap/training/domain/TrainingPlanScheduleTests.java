package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrainingPlanScheduleTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-26T15:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate START = LocalDate.of(2026, 8, 5);

	@Test
	void newPlansStartWithADraftScheduleAndNoRecurrenceMode() {
		TrainingPlan plan = plan();
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.DRAFT);
		assertThat(plan.recurrenceMode()).isNull();
		assertThat(plan.scheduleStartDate()).isNull();
		assertThat(plan.scheduleGeneratedThrough()).isNull();
		assertThat(plan.isScheduleActive()).isFalse();
	}

	@Test
	void activateSetsScheduleFieldsAndTimestamp() {
		TrainingPlan plan = plan();
		plan.activateSchedule(START, START.plusWeeks(8), " Europe/Stockholm ",
				TrainingPlanRecurrenceMode.REPEATING, CLOCK);

		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.ACTIVE);
		assertThat(plan.scheduleStartDate()).isEqualTo(START);
		assertThat(plan.scheduleEndDate()).isEqualTo(START.plusWeeks(8));
		assertThat(plan.scheduleTimezone()).isEqualTo("Europe/Stockholm");
		assertThat(plan.recurrenceMode()).isEqualTo(TrainingPlanRecurrenceMode.REPEATING);
		assertThat(plan.scheduleActivatedAt()).isEqualTo(CLOCK.instant());
		assertThat(plan.schedulePausedAt()).isNull();
		assertThat(plan.isScheduleActive()).isTrue();
	}

	@Test
	void lifecycleFollowsDraftActivePausedCompleted() {
		TrainingPlan plan = activated();

		plan.pauseSchedule(LATER);
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.PAUSED);
		assertThat(plan.schedulePausedAt()).isEqualTo(LATER.instant());

		plan.resumeSchedule(LATER);
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.ACTIVE);
		assertThat(plan.schedulePausedAt()).isNull();

		plan.completeSchedule(LATER);
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.COMPLETED);
	}

	@Test
	void pausedSchedulesCanBeCompletedDirectly() {
		TrainingPlan plan = activated();
		plan.pauseSchedule(LATER);
		plan.completeSchedule(LATER);
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.COMPLETED);
	}

	@Test
	void rejectsInvalidScheduleTransitions() {
		TrainingPlan draft = plan();
		assertThatThrownBy(() -> draft.pauseSchedule(CLOCK)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> draft.resumeSchedule(CLOCK)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> draft.completeSchedule(CLOCK)).isInstanceOf(IllegalStateException.class);

		TrainingPlan active = activated();
		assertThatThrownBy(() -> active.activateSchedule(
				START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, CLOCK))
				.isInstanceOf(IllegalStateException.class);

		TrainingPlan completed = activated();
		completed.completeSchedule(LATER);
		assertThatThrownBy(() -> completed.resumeSchedule(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.pauseSchedule(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.activateSchedule(
				START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, LATER))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void activateRejectsBlankTimezoneAndInvertedDates() {
		assertThatThrownBy(() -> plan().activateSchedule(
				START, null, "  ", TrainingPlanRecurrenceMode.FINITE, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> plan().activateSchedule(
				START, START.minusDays(1), "UTC", TrainingPlanRecurrenceMode.FINITE, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void generatedThroughClampsAndNeverMovesBackwards() {
		TrainingPlan plan = plan();
		plan.activateSchedule(START, START.plusDays(20), "UTC", TrainingPlanRecurrenceMode.REPEATING, CLOCK);

		plan.advanceGeneratedThrough(START.plusDays(10), null, LATER);
		assertThat(plan.scheduleGeneratedThrough()).isEqualTo(START.plusDays(10));

		plan.advanceGeneratedThrough(START.plusDays(5), null, LATER);
		assertThat(plan.scheduleGeneratedThrough()).isEqualTo(START.plusDays(10));

		plan.advanceGeneratedThrough(START.plusDays(60), null, LATER);
		assertThat(plan.scheduleGeneratedThrough()).isEqualTo(START.plusDays(20));
	}

	@Test
	void generatedThroughRespectsFiniteMaximum() {
		TrainingPlan plan = plan();
		plan.activateSchedule(START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, CLOCK);

		plan.advanceGeneratedThrough(START.plusDays(90), START.plusDays(13), LATER);
		assertThat(plan.scheduleGeneratedThrough()).isEqualTo(START.plusDays(13));
	}

	private static TrainingPlan activated() {
		TrainingPlan plan = plan();
		plan.activateSchedule(START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, CLOCK);
		return plan;
	}

	private static TrainingPlan plan() {
		return TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(UUID.randomUUID()),
				TrainingPlanType.STRENGTH,
				null,
				"Strength",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 12, 31),
				null,
				null,
				CLOCK);
	}

}
