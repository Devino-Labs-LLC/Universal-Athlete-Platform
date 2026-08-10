package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic primary-occurrence selection for client dashboards.
 *
 * <ol>
 *   <li>IN_PROGRESS</li>
 *   <li>earliest SCHEDULED</li>
 *   <li>most recently COMPLETED on the target date</li>
 *   <li>none</li>
 * </ol>
 */
public final class TrainingPrimaryOccurrenceResolver {

	private TrainingPrimaryOccurrenceResolver() {
	}

	public interface OccurrenceView {
		UUID occurrenceId();

		WorkoutOccurrenceStatus status();

		LocalDate scheduledDate();

		Instant startedAt();

		Instant completedAt();
	}

	public static Optional<OccurrenceView> resolve(List<? extends OccurrenceView> occurrences, LocalDate date) {
		Objects.requireNonNull(occurrences, "occurrences must not be null");
		Objects.requireNonNull(date, "date must not be null");
		List<? extends OccurrenceView> forDate = occurrences.stream()
				.filter(occurrence -> date.equals(occurrence.scheduledDate()))
				.toList();
		Optional<? extends OccurrenceView> inProgress = forDate.stream()
				.filter(occurrence -> occurrence.status() == WorkoutOccurrenceStatus.IN_PROGRESS)
				.min(Comparator
						.comparing(OccurrenceView::startedAt, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(OccurrenceView::occurrenceId));
		if (inProgress.isPresent()) {
			return Optional.of(inProgress.get());
		}
		Optional<? extends OccurrenceView> scheduled = forDate.stream()
				.filter(occurrence -> occurrence.status() == WorkoutOccurrenceStatus.SCHEDULED)
				.min(Comparator
						.comparing(OccurrenceView::scheduledDate)
						.thenComparing(OccurrenceView::occurrenceId));
		if (scheduled.isPresent()) {
			return Optional.of(scheduled.get());
		}
		return forDate.stream()
				.filter(occurrence -> occurrence.status() == WorkoutOccurrenceStatus.COMPLETED)
				.max(Comparator
						.comparing(OccurrenceView::completedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
						.thenComparing(OccurrenceView::occurrenceId))
				.map(OccurrenceView.class::cast);
	}

}
