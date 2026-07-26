package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutOccurrenceTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T16:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate DATE = LocalDate.of(2026, 7, 28);

	@Test
	void createsScheduledOccurrenceWithOptionalDetails() {
		WorkoutOccurrence occurrence = WorkoutOccurrence.createManual(
				WorkoutOccurrenceId.generate(),
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				DATE,
				LocalTime.of(8, 30),
				"  Focus on tempo  ",
				CLOCK);

		assertThat(occurrence.status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThat(occurrence.scheduledDate()).isEqualTo(DATE);
		assertThat(occurrence.plannedStartTime()).isEqualTo(LocalTime.of(8, 30));
		assertThat(occurrence.athleteNotes()).isEqualTo("Focus on tempo");
		assertThat(occurrence.startedAt()).isNull();
		assertThat(occurrence.completedAt()).isNull();
		assertThat(occurrence.origin()).isEqualTo(WorkoutOccurrenceOrigin.MANUAL);
		assertThat(occurrence.generationKey()).isNull();
		assertThat(occurrence.manuallyRescheduled()).isFalse();
		assertThat(occurrence.originalScheduledDate()).isNull();
	}

	@Test
	void createsGeneratedOccurrenceCarryingItsGenerationKey() {
		TrainingPlanId planId = TrainingPlanId.generate();
		WorkoutDayId dayId = WorkoutDayId.generate();
		WorkoutGenerationKey key = WorkoutGenerationKey.of(planId, dayId, DATE, 1);

		WorkoutOccurrence occurrence = WorkoutOccurrence.createGenerated(
				WorkoutOccurrenceId.generate(),
				planId,
				dayId,
				AthleteId.of(UUID.randomUUID()),
				DATE,
				LocalTime.of(6, 0),
				key,
				CLOCK);

		assertThat(occurrence.origin()).isEqualTo(WorkoutOccurrenceOrigin.GENERATED);
		assertThat(occurrence.generationKey()).isEqualTo(key);
		assertThat(occurrence.status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
	}

	@Test
	void lifecycleTransitionsAndTimestamps() {
		WorkoutOccurrence occurrence = createScheduled();

		occurrence.start(CLOCK);
		assertThat(occurrence.status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
		assertThat(occurrence.startedAt()).isEqualTo(CLOCK.instant());

		occurrence.start(LATER);
		assertThat(occurrence.startedAt()).isEqualTo(CLOCK.instant());

		occurrence.complete(LATER);
		assertThat(occurrence.status()).isEqualTo(WorkoutOccurrenceStatus.COMPLETED);
		assertThat(occurrence.completedAt()).isEqualTo(LATER.instant());
	}

	@Test
	void skipAndCancelClearCompletedAt() {
		WorkoutOccurrence scheduled = createScheduled();
		scheduled.skip(CLOCK);
		assertThat(scheduled.status()).isEqualTo(WorkoutOccurrenceStatus.SKIPPED);
		assertThat(scheduled.completedAt()).isNull();

		WorkoutOccurrence inProgress = createScheduled();
		inProgress.start(CLOCK);
		inProgress.skip(LATER);
		assertThat(inProgress.status()).isEqualTo(WorkoutOccurrenceStatus.SKIPPED);
		assertThat(inProgress.completedAt()).isNull();

		WorkoutOccurrence cancelled = createScheduled();
		cancelled.cancel(CLOCK);
		assertThat(cancelled.status()).isEqualTo(WorkoutOccurrenceStatus.CANCELLED);
		assertThat(cancelled.completedAt()).isNull();
	}

	@Test
	void updateDetailsAndChangeScheduledDateWhileMutable() {
		WorkoutOccurrence occurrence = createScheduled();
		occurrence.updateDetails(LocalTime.of(9, 0), "New note", CLOCK);
		assertThat(occurrence.plannedStartTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(occurrence.athleteNotes()).isEqualTo("New note");

		occurrence.changeScheduledDate(LocalDate.of(2026, 8, 1), LATER);
		assertThat(occurrence.scheduledDate()).isEqualTo(LocalDate.of(2026, 8, 1));
	}

	@Test
	void rescheduleRecordsOriginalDateOnceAndKeepsGenerationKey() {
		TrainingPlanId planId = TrainingPlanId.generate();
		WorkoutDayId dayId = WorkoutDayId.generate();
		WorkoutGenerationKey key = WorkoutGenerationKey.of(planId, dayId, DATE, 1);
		WorkoutOccurrence occurrence = WorkoutOccurrence.createGenerated(
				WorkoutOccurrenceId.generate(),
				planId,
				dayId,
				AthleteId.of(UUID.randomUUID()),
				DATE,
				LocalTime.of(6, 0),
				key,
				CLOCK);

		occurrence.reschedule(DATE.plusDays(1), LocalTime.of(7, 0), LATER);
		assertThat(occurrence.scheduledDate()).isEqualTo(DATE.plusDays(1));
		assertThat(occurrence.plannedStartTime()).isEqualTo(LocalTime.of(7, 0));
		assertThat(occurrence.manuallyRescheduled()).isTrue();
		assertThat(occurrence.originalScheduledDate()).isEqualTo(DATE);
		assertThat(occurrence.generationKey()).isEqualTo(key);

		occurrence.reschedule(DATE.plusDays(3), null, LATER);
		assertThat(occurrence.scheduledDate()).isEqualTo(DATE.plusDays(3));
		assertThat(occurrence.plannedStartTime()).isNull();
		assertThat(occurrence.originalScheduledDate()).isEqualTo(DATE);
	}

	@Test
	void rescheduleRejectedOutsideScheduledStatus() {
		WorkoutOccurrence occurrence = createScheduled();
		occurrence.start(CLOCK);
		assertThatThrownBy(() -> occurrence.reschedule(DATE.plusDays(1), null, LATER))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsInvalidTransitionsAndUpdatesAfterTerminalState() {
		WorkoutOccurrence occurrence = createScheduled();
		assertThatThrownBy(() -> occurrence.complete(LATER))
				.isInstanceOf(IllegalStateException.class);

		WorkoutOccurrence completed = createScheduled();
		completed.start(CLOCK);
		completed.complete(LATER);
		assertThatThrownBy(() -> completed.start(LATER))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.updateDetails(null, "x", LATER))
				.isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> WorkoutOccurrence.createManual(
				WorkoutOccurrenceId.generate(),
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				null,
				null,
				null,
				CLOCK))
				.isInstanceOf(NullPointerException.class);
	}

	private static WorkoutOccurrence createScheduled() {
		return WorkoutOccurrence.createManual(
				WorkoutOccurrenceId.generate(),
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				DATE,
				null,
				null,
				CLOCK);
	}

}
