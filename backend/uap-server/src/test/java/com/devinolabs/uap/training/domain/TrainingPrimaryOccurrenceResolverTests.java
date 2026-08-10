package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrainingPrimaryOccurrenceResolverTests {

	private static final LocalDate DATE = LocalDate.of(2026, 7, 31);

	@Test
	void prefersInProgressOverScheduledAndCompleted() {
		OccurrenceView scheduled = view("11111111-1111-1111-1111-111111111111", WorkoutOccurrenceStatus.SCHEDULED,
				null, null);
		OccurrenceView inProgress = view("22222222-2222-2222-2222-222222222222", WorkoutOccurrenceStatus.IN_PROGRESS,
				Instant.parse("2026-07-31T10:00:00Z"), null);
		OccurrenceView completed = view("33333333-3333-3333-3333-333333333333", WorkoutOccurrenceStatus.COMPLETED,
				Instant.parse("2026-07-31T08:00:00Z"), Instant.parse("2026-07-31T09:00:00Z"));

		Optional<TrainingPrimaryOccurrenceResolver.OccurrenceView> resolved =
				TrainingPrimaryOccurrenceResolver.resolve(List.of(scheduled, completed, inProgress), DATE);

		assertThat(resolved).isPresent();
		assertThat(resolved.get().occurrenceId()).isEqualTo(inProgress.occurrenceId());
	}

	@Test
	void prefersEarliestScheduledWhenNoInProgress() {
		OccurrenceView later = view("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", WorkoutOccurrenceStatus.SCHEDULED,
				null, null);
		OccurrenceView earlier = view("99999999-9999-9999-9999-999999999999", WorkoutOccurrenceStatus.SCHEDULED,
				null, null);

		Optional<TrainingPrimaryOccurrenceResolver.OccurrenceView> resolved =
				TrainingPrimaryOccurrenceResolver.resolve(List.of(later, earlier), DATE);

		assertThat(resolved).isPresent();
		assertThat(resolved.get().occurrenceId()).isEqualTo(earlier.occurrenceId());
	}

	@Test
	void fallsBackToMostRecentlyCompleted() {
		OccurrenceView older = view("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", WorkoutOccurrenceStatus.COMPLETED,
				Instant.parse("2026-07-31T07:00:00Z"), Instant.parse("2026-07-31T08:00:00Z"));
		OccurrenceView newer = view("cccccccc-cccc-cccc-cccc-cccccccccccc", WorkoutOccurrenceStatus.COMPLETED,
				Instant.parse("2026-07-31T09:00:00Z"), Instant.parse("2026-07-31T10:00:00Z"));

		Optional<TrainingPrimaryOccurrenceResolver.OccurrenceView> resolved =
				TrainingPrimaryOccurrenceResolver.resolve(List.of(older, newer), DATE);

		assertThat(resolved).isPresent();
		assertThat(resolved.get().occurrenceId()).isEqualTo(newer.occurrenceId());
	}

	@Test
	void returnsEmptyWhenNoEligibleOccurrencesOnDate() {
		OccurrenceView otherDate = new OccurrenceView(
				UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
				WorkoutOccurrenceStatus.SCHEDULED,
				LocalDate.of(2026, 7, 30),
				null,
				null);

		assertThat(TrainingPrimaryOccurrenceResolver.resolve(List.of(otherDate), DATE)).isEmpty();
		assertThat(TrainingPrimaryOccurrenceResolver.resolve(List.of(), DATE)).isEmpty();
	}

	private static OccurrenceView view(
			String id,
			WorkoutOccurrenceStatus status,
			Instant startedAt,
			Instant completedAt) {
		return new OccurrenceView(UUID.fromString(id), status, DATE, startedAt, completedAt);
	}

	private record OccurrenceView(
			UUID occurrenceId,
			WorkoutOccurrenceStatus status,
			LocalDate scheduledDate,
			Instant startedAt,
			Instant completedAt) implements TrainingPrimaryOccurrenceResolver.OccurrenceView {
	}

}
