package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutSessionTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T18:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T19:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsNotStartedSessionAndSupportsExecutionUpdates() {
		WorkoutSession session = create();
		assertThat(session.status()).isEqualTo(WorkoutSessionStatus.NOT_STARTED);
		assertThat(session.completedAt()).isNull();

		session.start(CLOCK);
		session.updateExecution(
				4, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, 90, 8, LATER);
		session.updateNotes("  Felt strong  ", LATER);

		assertThat(session.status()).isEqualTo(WorkoutSessionStatus.IN_PROGRESS);
		assertThat(session.actualSets()).isEqualTo(4);
		assertThat(session.actualReps()).isEqualTo(5);
		assertThat(session.actualWeight()).isEqualByComparingTo("100");
		assertThat(session.athleteNotes()).isEqualTo("Felt strong");
	}

	@Test
	void rejectsInvalidExecutionValues() {
		WorkoutSession session = create();
		session.start(CLOCK);

		assertThatThrownBy(() -> session.updateExecution(
				-1, null, null, null, null, null, null, null, null, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualSets");

		assertThatThrownBy(() -> session.updateExecution(
				3, null, new BigDecimal("10"), null, null, null, null, null, null, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("weightUnit");

		assertThatThrownBy(() -> session.updateExecution(
				3, null, null, null, null, null, null, null, 11, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualRpe");
	}

	@Test
	void supportsValidLifecycleAndRejectsInvalidTransitions() {
		WorkoutSession completed = create();
		completed.start(CLOCK);
		completed.complete(LATER);
		assertThat(completed.status()).isEqualTo(WorkoutSessionStatus.COMPLETED);
		assertThat(completed.completedAt()).isEqualTo(Instant.parse("2026-07-25T19:00:00Z"));

		WorkoutSession skipFromNotStarted = create();
		skipFromNotStarted.skip(LATER);
		assertThat(skipFromNotStarted.status()).isEqualTo(WorkoutSessionStatus.SKIPPED);
		assertThat(skipFromNotStarted.completedAt()).isNull();

		WorkoutSession skipFromInProgress = create();
		skipFromInProgress.start(CLOCK);
		skipFromInProgress.skip(LATER);
		assertThat(skipFromInProgress.status()).isEqualTo(WorkoutSessionStatus.SKIPPED);

		WorkoutSession notStarted = create();
		assertThatThrownBy(() -> notStarted.complete(LATER)).isInstanceOf(IllegalStateException.class);

		WorkoutSession finished = create();
		finished.start(CLOCK);
		finished.complete(LATER);
		assertThatThrownBy(() -> finished.skip(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> finished.start(LATER)).isInstanceOf(IllegalStateException.class);
	}

	private static WorkoutSession create() {
		return WorkoutSession.create(
				WorkoutSessionId.generate(),
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				CLOCK);
	}

}
