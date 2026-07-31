package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

final class WorkoutOccurrenceLoadSummaryFetchSupport {

	private WorkoutOccurrenceLoadSummaryFetchSupport() {
	}

	static WorkoutOccurrenceLoadSummaryJpaEntity loadWithChildren(
			WorkoutOccurrenceLoadSummaryJpaRepository repository,
			UUID occurrenceId,
			UUID athleteId) {
		WorkoutOccurrenceLoadSummaryJpaEntity withCategories = repository
				.findByOccurrenceIdAndAthleteIdWithCategories(occurrenceId, athleteId)
				.orElseThrow();
		repository.findByIdWithMovements(withCategories.getId());
		return withCategories;
	}

	static WorkoutOccurrenceLoadSummaryJpaEntity loadWithChildren(
			WorkoutOccurrenceLoadSummaryJpaRepository repository,
			UUID summaryId) {
		WorkoutOccurrenceLoadSummaryJpaEntity withCategories = repository.findByIdsWithCategories(List.of(summaryId))
				.stream()
				.findFirst()
				.orElseThrow();
		repository.findByIdsWithMovements(List.of(summaryId));
		return withCategories;
	}

	static List<WorkoutOccurrenceLoadSummaryJpaEntity> loadFilteredWithChildren(
			WorkoutOccurrenceLoadSummaryJpaRepository repository,
			List<WorkoutOccurrenceLoadSummaryJpaEntity> summaries) {
		if (summaries.isEmpty()) {
			return summaries;
		}
		List<UUID> ids = summaries.stream().map(WorkoutOccurrenceLoadSummaryJpaEntity::getId).toList();
		repository.findByIdsWithCategories(ids);
		repository.findByIdsWithMovements(ids);
		return summaries;
	}

}
