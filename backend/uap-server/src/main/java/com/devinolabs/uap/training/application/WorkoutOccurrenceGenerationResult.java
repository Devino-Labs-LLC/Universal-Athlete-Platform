package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Outcome of materialising schedule placements into dated occurrences.
 *
 * @param createdCount placements newly written in this run
 * @param existingCount placements already materialised (idempotent re-runs, manual collisions)
 * @param cancelledPlacementCount placements suppressed by a cancelled tombstone
 * @param outOfScheduleCount placements inside the requested range but past the schedule end
 */
public record WorkoutOccurrenceGenerationResult(
		LocalDate from,
		LocalDate to,
		int createdCount,
		int existingCount,
		int cancelledPlacementCount,
		int outOfScheduleCount,
		LocalDate scheduleGeneratedThrough,
		List<WorkoutOccurrenceResult> createdOccurrences) {
}
