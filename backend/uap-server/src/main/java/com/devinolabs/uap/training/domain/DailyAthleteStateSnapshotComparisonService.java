package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Factual field-level comparison between two immutable snapshots.
 * Does not interpret favorability.
 */
public final class DailyAthleteStateSnapshotComparisonService {

	private DailyAthleteStateSnapshotComparisonService() {
	}

	public static DailyAthleteStateSnapshotComparison compare(
			DailyAthleteStateSnapshot older,
			DailyAthleteStateSnapshot newer) {
		Objects.requireNonNull(older, "older must not be null");
		Objects.requireNonNull(newer, "newer must not be null");
		if (!older.athleteId().equals(newer.athleteId())) {
			throw new IllegalArgumentException("Snapshots must belong to the same athlete");
		}

		List<FieldDifference> differences = new ArrayList<>();
		add(differences, "sleepDurationMinutes", older.sleepDurationMinutes(), newer.sleepDurationMinutes());
		add(differences, "sleepQuality", older.sleepQuality(), newer.sleepQuality());
		add(differences, "fatigue", older.fatigue(), newer.fatigue());
		add(differences, "muscleSoreness", older.muscleSoreness(), newer.muscleSoreness());
		add(differences, "stress", older.stress(), newer.stress());
		add(differences, "mood", older.mood(), newer.mood());
		add(differences, "motivation", older.motivation(), newer.motivation());
		add(differences, "checkInPresent", older.checkInPresent(), newer.checkInPresent());
		add(differences, "recoveryCheckInVersion", older.recoveryCheckInVersion(), newer.recoveryCheckInVersion());

		boolean recoveryChanged = differences.stream().anyMatch(d -> d.field().startsWith("sleep")
				|| d.field().equals("fatigue")
				|| d.field().equals("muscleSoreness")
				|| d.field().equals("stress")
				|| d.field().equals("mood")
				|| d.field().equals("motivation")
				|| d.field().equals("checkInPresent")
				|| d.field().equals("recoveryCheckInVersion"));

		boolean discomfortChanged = !Objects.equals(
				canonicalizeDiscomfort(older.discomfortObservations()),
				canonicalizeDiscomfort(newer.discomfortObservations()));
		if (discomfortChanged) {
			differences.add(new FieldDifference(
					"discomfortObservations",
					canonicalizeDiscomfort(older.discomfortObservations()),
					canonicalizeDiscomfort(newer.discomfortObservations())));
		}

		boolean baselineChanged = !Objects.equals(
				canonicalizeMetrics(older.recoveryMetrics()),
				canonicalizeMetrics(newer.recoveryMetrics()))
				|| older.baselineWindowDays() != newer.baselineWindowDays()
				|| older.recoveryAnalyticsCalculationVersion() != newer.recoveryAnalyticsCalculationVersion();
		if (baselineChanged) {
			differences.add(new FieldDifference(
					"recoveryMetrics",
					canonicalizeMetrics(older.recoveryMetrics()),
					canonicalizeMetrics(newer.recoveryMetrics())));
			add(differences, "baselineWindowDays", older.baselineWindowDays(), newer.baselineWindowDays());
			add(differences, "analyticsVersion",
					older.recoveryAnalyticsCalculationVersion(),
					newer.recoveryAnalyticsCalculationVersion());
		}

		add(differences, "occurrenceCount", older.occurrenceCount(), newer.occurrenceCount());
		add(differences, "completedOccurrenceCount", older.completedOccurrenceCount(), newer.completedOccurrenceCount());
		add(differences, "totalVolumeKilograms", older.totalVolumeKilograms(), newer.totalVolumeKilograms());
		add(differences, "totalDistanceMeters", older.totalDistanceMeters(), newer.totalDistanceMeters());
		add(differences, "totalDurationSeconds", older.totalDurationSeconds(), newer.totalDurationSeconds());
		add(differences, "totalSessionRpeLoad", older.totalSessionRpeLoad(), newer.totalSessionRpeLoad());
		add(differences, "averageSessionRpe", older.averageSessionRpe(), newer.averageSessionRpe());
		add(differences, "completedExerciseCount", older.completedExerciseCount(), newer.completedExerciseCount());
		add(differences, "completedSetCount", older.completedSetCount(), newer.completedSetCount());

		boolean trainingLoadChanged = differences.stream().anyMatch(d -> d.field().equals("occurrenceCount")
				|| d.field().equals("completedOccurrenceCount")
				|| d.field().equals("totalVolumeKilograms")
				|| d.field().equals("totalDistanceMeters")
				|| d.field().equals("totalDurationSeconds")
				|| d.field().equals("totalSessionRpeLoad")
				|| d.field().equals("averageSessionRpe")
				|| d.field().equals("completedExerciseCount")
				|| d.field().equals("completedSetCount"))
				|| !Objects.equals(
						canonicalizeCategories(older.categorySummaries()),
						canonicalizeCategories(newer.categorySummaries()))
				|| !Objects.equals(
						canonicalizeMovements(older.movementSummaries()),
						canonicalizeMovements(newer.movementSummaries()));

		add(differences, "scheduledOccurrenceCount", older.scheduledOccurrenceCount(), newer.scheduledOccurrenceCount());
		add(differences, "completedScheduledCount", older.completedScheduledCount(), newer.completedScheduledCount());
		add(differences, "skippedScheduledCount", older.skippedScheduledCount(), newer.skippedScheduledCount());
		add(differences, "cancelledScheduledCount", older.cancelledScheduledCount(), newer.cancelledScheduledCount());
		add(differences, "inProgressScheduledCount", older.inProgressScheduledCount(), newer.inProgressScheduledCount());
		boolean scheduleChanged = differences.stream().anyMatch(d -> d.field().startsWith("scheduled")
				|| d.field().equals("completedScheduledCount")
				|| d.field().equals("skippedScheduledCount")
				|| d.field().equals("cancelledScheduledCount")
				|| d.field().equals("inProgressScheduledCount"))
				|| !Objects.equals(
						canonicalizeSchedule(older.scheduledOccurrences()),
						canonicalizeSchedule(newer.scheduledOccurrences()));

		return new DailyAthleteStateSnapshotComparison(
				older.id(),
				newer.id(),
				older.stateDate(),
				newer.stateDate(),
				older.snapshotVersion(),
				newer.snapshotVersion(),
				recoveryChanged,
				baselineChanged,
				trainingLoadChanged,
				scheduleChanged,
				discomfortChanged,
				List.copyOf(differences));
	}

	private static void add(List<FieldDifference> differences, String field, Object previous, Object next) {
		if (!Objects.equals(previous, next)) {
			differences.add(new FieldDifference(field, stringify(previous), stringify(next)));
		}
	}

	private static String stringify(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String canonicalizeDiscomfort(List<DailyAthleteStateDiscomfortSnapshot> discomfort) {
		return discomfort.stream()
				.sorted(DailyAthleteStateFingerprintCalculator.discomfortOrder())
				.map(d -> d.bodyArea() + "/" + d.bodySide() + "/" + d.intensity() + "/" + nullSafe(d.notes()))
				.collect(Collectors.joining("|"));
	}

	private static String canonicalizeMetrics(List<DailyAthleteStateRecoveryMetricSnapshot> metrics) {
		return metrics.stream()
				.sorted(DailyAthleteStateFingerprintCalculator.metricOrder())
				.map(m -> m.metricType() + ":"
						+ m.observationCount() + ":"
						+ m.baselineMean() + ":"
						+ m.comparisonBand() + ":"
						+ m.targetValue() + ":"
						+ m.standardizedDeviation())
				.collect(Collectors.joining("|"));
	}

	private static String canonicalizeCategories(List<DailyAthleteStateCategorySummarySnapshot> categories) {
		return categories.stream()
				.sorted(Comparator.comparing(c -> c.category().name()))
				.map(c -> c.category() + ":" + c.volumeKilograms() + ":" + c.completedExerciseCount())
				.collect(Collectors.joining("|"));
	}

	private static String canonicalizeMovements(List<DailyAthleteStateMovementSummarySnapshot> movements) {
		return movements.stream()
				.sorted(Comparator.comparing(m -> m.movementPattern().name()))
				.map(m -> m.movementPattern() + ":" + m.volumeKilograms() + ":" + m.completedRepetitionCount())
				.collect(Collectors.joining("|"));
	}

	private static String canonicalizeSchedule(List<DailyAthleteStateScheduledOccurrenceSnapshot> schedule) {
		return schedule.stream()
				.sorted(DailyAthleteStateFingerprintCalculator.scheduleOrder())
				.map(s -> s.occurrenceId() + ":" + s.occurrenceStatus())
				.collect(Collectors.joining("|"));
	}

	private static String nullSafe(String value) {
		return value == null ? "" : value;
	}

	public record FieldDifference(String field, String previousValue, String newValue) {
	}

	public record DailyAthleteStateSnapshotComparison(
			DailyAthleteStateSnapshotId olderSnapshotId,
			DailyAthleteStateSnapshotId newerSnapshotId,
			java.time.LocalDate olderStateDate,
			java.time.LocalDate newerStateDate,
			int olderVersion,
			int newerVersion,
			boolean recoveryChanged,
			boolean baselineChanged,
			boolean trainingLoadChanged,
			boolean scheduleChanged,
			boolean discomfortChanged,
			List<FieldDifference> fieldDifferences) {
	}

}
