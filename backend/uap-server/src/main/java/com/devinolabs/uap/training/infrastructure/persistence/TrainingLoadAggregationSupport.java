package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutLoadCategorySummary;
import com.devinolabs.uap.training.domain.WorkoutLoadMovementPatternSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

final class TrainingLoadAggregationSupport {

	private static final BigDecimal ZERO_VOLUME = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
	private static final BigDecimal ZERO_DISTANCE = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
	private static final BigDecimal ZERO_LOAD = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

	private TrainingLoadAggregationSupport() {
	}

	static List<DailyTrainingLoadSummary> aggregateDaily(
			List<WorkoutOccurrenceLoadSummary> summaries,
			LocalDate startDate,
			LocalDate endDate,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		Map<LocalDate, List<WorkoutOccurrenceLoadSummary>> byDate = new TreeMap<>();
		for (WorkoutOccurrenceLoadSummary summary : summaries) {
			if (summary.scheduledDate().isBefore(startDate) || summary.scheduledDate().isAfter(endDate)) {
				continue;
			}
			if (!matchesFilters(summary, category, movementPattern)) {
				continue;
			}
			byDate.computeIfAbsent(summary.scheduledDate(), ignored -> new ArrayList<>()).add(summary);
		}
		List<DailyTrainingLoadSummary> results = new ArrayList<>(byDate.size());
		for (Map.Entry<LocalDate, List<WorkoutOccurrenceLoadSummary>> entry : byDate.entrySet()) {
			results.add(toDaily(entry.getKey(), entry.getValue(), category, movementPattern));
		}
		return results;
	}

	static List<WeeklyTrainingLoadSummary> aggregateWeekly(
			List<WorkoutOccurrenceLoadSummary> summaries,
			LocalDate startDate,
			LocalDate endDate,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		Map<LocalDate, List<WorkoutOccurrenceLoadSummary>> byWeekStart = new TreeMap<>();
		for (WorkoutOccurrenceLoadSummary summary : summaries) {
			if (summary.scheduledDate().isBefore(startDate) || summary.scheduledDate().isAfter(endDate)) {
				continue;
			}
			if (!matchesFilters(summary, category, movementPattern)) {
				continue;
			}
			LocalDate weekStart = summary.scheduledDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			byWeekStart.computeIfAbsent(weekStart, ignored -> new ArrayList<>()).add(summary);
		}
		List<WeeklyTrainingLoadSummary> results = new ArrayList<>(byWeekStart.size());
		for (Map.Entry<LocalDate, List<WorkoutOccurrenceLoadSummary>> entry : byWeekStart.entrySet()) {
			LocalDate weekStart = entry.getKey();
			results.add(toWeekly(weekStart, weekStart.plusDays(6), entry.getValue(), category, movementPattern));
		}
		return results;
	}

	private static boolean matchesFilters(
			WorkoutOccurrenceLoadSummary summary,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		if (category != null && summary.categorySummaries().stream().noneMatch(c -> c.category() == category)) {
			return false;
		}
		if (movementPattern != null
				&& summary.movementSummaries().stream().noneMatch(m -> m.primaryMovementPattern() == movementPattern)) {
			return false;
		}
		return true;
	}

	private static DailyTrainingLoadSummary toDaily(
			LocalDate date,
			List<WorkoutOccurrenceLoadSummary> summaries,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		Accumulator accumulator = accumulate(summaries, category, movementPattern);
		return new DailyTrainingLoadSummary(
				date,
				summaries.size(),
				accumulator.ratedOccurrenceCount,
				summaries.size() - accumulator.ratedOccurrenceCount,
				accumulator.completedExerciseCount,
				accumulator.completedSetCount,
				accumulator.completedRepetitionCount,
				accumulator.totalVolumeKilograms,
				accumulator.totalDurationSeconds,
				accumulator.totalDistanceMeters,
				accumulator.totalSessionRpeLoad,
				accumulator.averageSessionRpe(),
				accumulator.totalSessionDurationMinutes,
				accumulator.noImpactExerciseCount,
				accumulator.lowImpactExerciseCount,
				accumulator.moderateImpactExerciseCount,
				accumulator.highImpactExerciseCount,
				accumulator.categorySummaries(),
				accumulator.movementSummaries());
	}

	private static WeeklyTrainingLoadSummary toWeekly(
			LocalDate weekStart,
			LocalDate weekEnd,
			List<WorkoutOccurrenceLoadSummary> summaries,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		Accumulator accumulator = accumulate(summaries, category, movementPattern);
		long trainingDays = summaries.stream().map(WorkoutOccurrenceLoadSummary::scheduledDate).distinct().count();
		return new WeeklyTrainingLoadSummary(
				weekStart,
				weekEnd,
				summaries.size(),
				trainingDays,
				accumulator.ratedOccurrenceCount,
				summaries.size() - accumulator.ratedOccurrenceCount,
				accumulator.completedExerciseCount,
				accumulator.completedSetCount,
				accumulator.completedRepetitionCount,
				accumulator.totalVolumeKilograms,
				accumulator.totalDurationSeconds,
				accumulator.totalDistanceMeters,
				accumulator.totalSessionRpeLoad,
				accumulator.averageSessionRpe(),
				accumulator.totalSessionDurationMinutes,
				accumulator.highestSessionRpe,
				accumulator.noImpactExerciseCount,
				accumulator.lowImpactExerciseCount,
				accumulator.moderateImpactExerciseCount,
				accumulator.highImpactExerciseCount,
				accumulator.categorySummaries(),
				accumulator.movementSummaries());
	}

	private static Accumulator accumulate(
			List<WorkoutOccurrenceLoadSummary> summaries,
			ExerciseDefinitionCategory categoryFilter,
			MovementPattern movementFilter) {
		Accumulator accumulator = new Accumulator();
		for (WorkoutOccurrenceLoadSummary summary : summaries) {
			if (categoryFilter == null && movementFilter == null) {
				accumulator.addOccurrence(summary);
			}
			else {
				accumulator.addFilteredOccurrence(summary, categoryFilter, movementFilter);
			}
		}
		return accumulator;
	}

	private static final class Accumulator {

		private long ratedOccurrenceCount;
		private BigDecimal sessionRpeSum = BigDecimal.ZERO;
		private BigDecimal highestSessionRpe;
		private long totalSessionDurationMinutes;
		private BigDecimal totalSessionRpeLoad = ZERO_LOAD;

		private long completedExerciseCount;
		private long completedSetCount;
		private long completedRepetitionCount;
		private BigDecimal totalVolumeKilograms = ZERO_VOLUME;
		private long totalDurationSeconds;
		private BigDecimal totalDistanceMeters = ZERO_DISTANCE;
		private long noImpactExerciseCount;
		private long lowImpactExerciseCount;
		private long moderateImpactExerciseCount;
		private long highImpactExerciseCount;

		private final Map<ExerciseDefinitionCategory, CategoryAccumulator> categories = new HashMap<>();
		private final Map<MovementPattern, MovementAccumulator> movements = new HashMap<>();

		private void addOccurrence(WorkoutOccurrenceLoadSummary summary) {
			addSessionFields(summary);
			completedExerciseCount += summary.completedExerciseCount();
			completedSetCount += summary.completedSetCount();
			completedRepetitionCount += summary.completedRepetitionCount();
			totalVolumeKilograms = totalVolumeKilograms.add(summary.totalVolumeKilograms());
			totalDurationSeconds += summary.totalDurationSeconds();
			totalDistanceMeters = totalDistanceMeters.add(summary.totalDistanceMeters());
			noImpactExerciseCount += summary.noImpactExerciseCount();
			lowImpactExerciseCount += summary.lowImpactExerciseCount();
			moderateImpactExerciseCount += summary.moderateImpactExerciseCount();
			highImpactExerciseCount += summary.highImpactExerciseCount();
			for (WorkoutLoadCategorySummary category : summary.categorySummaries()) {
				categories.computeIfAbsent(category.category(), CategoryAccumulator::new).add(category);
			}
			for (WorkoutLoadMovementPatternSummary movement : summary.movementSummaries()) {
				movements.computeIfAbsent(movement.primaryMovementPattern(), MovementAccumulator::new).add(movement);
			}
		}

		private void addFilteredOccurrence(
				WorkoutOccurrenceLoadSummary summary,
				ExerciseDefinitionCategory categoryFilter,
				MovementPattern movementFilter) {
			addSessionFields(summary);
			if (categoryFilter != null) {
				summary.categorySummaries().stream()
						.filter(category -> category.category() == categoryFilter)
						.forEach(category -> {
							completedExerciseCount += category.completedExerciseCount();
							completedSetCount += category.completedSetCount();
							totalVolumeKilograms = totalVolumeKilograms.add(category.volumeKilograms());
							totalDurationSeconds += category.durationSeconds();
							totalDistanceMeters = totalDistanceMeters.add(category.distanceMeters());
							categories.computeIfAbsent(category.category(), CategoryAccumulator::new).add(category);
						});
			}
			if (movementFilter != null) {
				summary.movementSummaries().stream()
						.filter(movement -> movement.primaryMovementPattern() == movementFilter)
						.forEach(movement -> {
							completedExerciseCount += movement.completedExerciseCount();
							completedSetCount += movement.completedSetCount();
							completedRepetitionCount += movement.completedRepetitionCount();
							totalVolumeKilograms = totalVolumeKilograms.add(movement.volumeKilograms());
							totalDurationSeconds += movement.durationSeconds();
							totalDistanceMeters = totalDistanceMeters.add(movement.distanceMeters());
							movements.computeIfAbsent(movement.primaryMovementPattern(), MovementAccumulator::new)
									.add(movement);
						});
			}
		}

		private void addSessionFields(WorkoutOccurrenceLoadSummary summary) {
			SessionRpe sessionRpe = summary.sessionRpe();
			if (sessionRpe != null) {
				ratedOccurrenceCount++;
				sessionRpeSum = sessionRpeSum.add(sessionRpe.value());
				if (highestSessionRpe == null || sessionRpe.value().compareTo(highestSessionRpe) > 0) {
					highestSessionRpe = sessionRpe.value();
				}
			}
			if (summary.sessionDurationMinutes() != null) {
				totalSessionDurationMinutes += summary.sessionDurationMinutes();
			}
			if (summary.sessionRpeLoad() != null) {
				totalSessionRpeLoad = totalSessionRpeLoad.add(summary.sessionRpeLoad().value());
			}
		}

		private BigDecimal averageSessionRpe() {
			if (ratedOccurrenceCount == 0) {
				return null;
			}
			return sessionRpeSum.divide(BigDecimal.valueOf(ratedOccurrenceCount), 2, RoundingMode.HALF_UP);
		}

		private List<WorkoutLoadCategorySummary> categorySummaries() {
			return categories.values().stream()
					.sorted(Comparator.comparing(a -> a.category.name()))
					.map(CategoryAccumulator::toSummary)
					.toList();
		}

		private List<WorkoutLoadMovementPatternSummary> movementSummaries() {
			return movements.values().stream()
					.sorted(Comparator.comparing(a -> a.pattern.name()))
					.map(MovementAccumulator::toSummary)
					.toList();
		}
	}

	private static final class CategoryAccumulator {

		private final ExerciseDefinitionCategory category;
		private long exerciseCount;
		private long setCount;
		private BigDecimal volume = ZERO_VOLUME;
		private long durationSeconds;
		private BigDecimal distance = ZERO_DISTANCE;

		private CategoryAccumulator(ExerciseDefinitionCategory category) {
			this.category = category;
		}

		private void add(WorkoutLoadCategorySummary summary) {
			exerciseCount += summary.completedExerciseCount();
			setCount += summary.completedSetCount();
			volume = volume.add(summary.volumeKilograms());
			durationSeconds += summary.durationSeconds();
			distance = distance.add(summary.distanceMeters());
		}

		private WorkoutLoadCategorySummary toSummary() {
			return new WorkoutLoadCategorySummary(category, exerciseCount, setCount, volume, durationSeconds, distance);
		}
	}

	private static final class MovementAccumulator {

		private final MovementPattern pattern;
		private long exerciseCount;
		private long setCount;
		private long repetitionCount;
		private BigDecimal volume = ZERO_VOLUME;
		private long durationSeconds;
		private BigDecimal distance = ZERO_DISTANCE;

		private MovementAccumulator(MovementPattern pattern) {
			this.pattern = pattern;
		}

		private void add(WorkoutLoadMovementPatternSummary summary) {
			exerciseCount += summary.completedExerciseCount();
			setCount += summary.completedSetCount();
			repetitionCount += summary.completedRepetitionCount();
			volume = volume.add(summary.volumeKilograms());
			durationSeconds += summary.durationSeconds();
			distance = distance.add(summary.distanceMeters());
		}

		private WorkoutLoadMovementPatternSummary toSummary() {
			return new WorkoutLoadMovementPatternSummary(
					pattern, exerciseCount, setCount, repetitionCount, volume, durationSeconds, distance);
		}
	}

}
