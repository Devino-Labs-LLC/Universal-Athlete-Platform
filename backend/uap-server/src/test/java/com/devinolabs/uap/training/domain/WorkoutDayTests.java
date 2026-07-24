package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutDayTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsPlannedDayWithNormalizedTitle() {
		WorkoutDay day = WorkoutDay.create(
				WorkoutDayId.generate(),
				TrainingPlanId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"  Lower   Body  ",
				"  Squats focus  ",
				DayOfWeek.MONDAY,
				LocalTime.of(9, 0),
				60,
				CLOCK);

		assertThat(day.title()).isEqualTo("Lower   Body");
		assertThat(day.normalizedTitle()).isEqualTo("lower body");
		assertThat(day.description()).isEqualTo("Squats focus");
		assertThat(day.status()).isEqualTo(WorkoutDayStatus.PLANNED);
		assertThat(day.scheduledDay()).isEqualTo(DayOfWeek.MONDAY);
		assertThat(day.expectedDurationMinutes()).isEqualTo(60);
	}

	@Test
	void rejectsInvalidTitleOrderAndDuration() {
		assertThatThrownBy(() -> WorkoutDay.create(
				WorkoutDayId.generate(), TrainingPlanId.generate(), AthleteId.of(UUID.randomUUID()),
				-1, "Title", null, DayOfWeek.TUESDAY, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> WorkoutDay.create(
				WorkoutDayId.generate(), TrainingPlanId.generate(), AthleteId.of(UUID.randomUUID()),
				0, "  ", null, DayOfWeek.TUESDAY, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> WorkoutDay.create(
				WorkoutDayId.generate(), TrainingPlanId.generate(), AthleteId.of(UUID.randomUUID()),
				0, "Title", null, DayOfWeek.TUESDAY, null, 0, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void supportsValidLifecycleAndRejectsInvalidTransitions() {
		WorkoutDay day = createPlanned();
		day.activate(LATER);
		assertThat(day.status()).isEqualTo(WorkoutDayStatus.ACTIVE);
		day.complete(LATER);
		assertThat(day.status()).isEqualTo(WorkoutDayStatus.COMPLETED);

		WorkoutDay skipFromPlanned = createPlanned();
		skipFromPlanned.skip(LATER);
		assertThat(skipFromPlanned.status()).isEqualTo(WorkoutDayStatus.SKIPPED);

		WorkoutDay skipFromActive = createPlanned();
		skipFromActive.activate(LATER);
		skipFromActive.skip(LATER);
		assertThat(skipFromActive.status()).isEqualTo(WorkoutDayStatus.SKIPPED);

		WorkoutDay planned = createPlanned();
		assertThatThrownBy(() -> planned.complete(LATER)).isInstanceOf(IllegalStateException.class);

		WorkoutDay completed = createPlanned();
		completed.activate(LATER);
		completed.complete(LATER);
		assertThatThrownBy(() -> completed.skip(LATER)).isInstanceOf(IllegalStateException.class);
	}

	private static WorkoutDay createPlanned() {
		return WorkoutDay.create(
				WorkoutDayId.generate(),
				TrainingPlanId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"Lower Body",
				null,
				DayOfWeek.MONDAY,
				null,
				null,
				CLOCK);
	}

}
