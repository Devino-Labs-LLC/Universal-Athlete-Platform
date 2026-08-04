package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyAthleteStateCategorySummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateDateOutOfRangeException;
import com.devinolabs.uap.training.domain.DailyAthleteStateDiscomfortSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateFingerprintCalculator;
import com.devinolabs.uap.training.domain.DailyAthleteStateMovementSummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateRecoveryMetricSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateScheduledOccurrenceSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotFactory.AssembledDailyAthleteStateSource;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateBaselineWindowException;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateDateException;
import com.devinolabs.uap.training.domain.InvalidRecoveryBaselineWindowException;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;
import com.devinolabs.uap.training.domain.RecoveryBaselineCalculator;
import com.devinolabs.uap.training.domain.WorkoutLoadCategorySummary;
import com.devinolabs.uap.training.domain.WorkoutLoadMovementPatternSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;

final class DailyAthleteStateSupport {

	static final int MAX_HISTORY_DAYS = 366;
	static final RecoveryAnalyticsCalculationVersion ANALYTICS_VERSION =
			RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1;

	private DailyAthleteStateSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireReadableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static void requireStateDate(LocalDate stateDate, Clock clock) {
		Objects.requireNonNull(stateDate, "stateDate must not be null");
		LocalDate today = LocalDate.now(clock);
		if (stateDate.isAfter(today)) {
			throw new DailyAthleteStateDateOutOfRangeException("stateDate must not be in the future");
		}
		long ageDays = ChronoUnit.DAYS.between(stateDate, today);
		if (ageDays > MAX_HISTORY_DAYS) {
			throw new DailyAthleteStateDateOutOfRangeException(
					"stateDate must be within the last " + MAX_HISTORY_DAYS + " days");
		}
	}

	static void requireBaselineWindow(int baselineWindowDays) {
		try {
			RecoveryBaselineCalculator.requireSupportedWindow(baselineWindowDays);
		}
		catch (InvalidRecoveryBaselineWindowException ex) {
			throw new InvalidDailyAthleteStateBaselineWindowException(ex.getMessage());
		}
	}

	static void requireHistoryRange(LocalDate startDate, LocalDate endDate) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new InvalidDailyAthleteStateDateException("endDate must not be before startDate");
		}
		long span = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (span > MAX_HISTORY_DAYS) {
			throw new InvalidDailyAthleteStateDateException(
					"Date range must not exceed " + MAX_HISTORY_DAYS + " days");
		}
	}

	static AssembledDailyAthleteStateSource assemble(
			AthleteId athleteId,
			LocalDate stateDate,
			int baselineWindowDays,
			DailyRecoveryCheckInRepository checkInRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			WorkoutOccurrenceRepository occurrenceRepository,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			Clock clock) {
		Optional<DailyRecoveryCheckIn> checkIn = checkInRepository.findByAthleteIdAndCheckInDate(athleteId, stateDate);
		List<DailyRecoveryCheckIn> priorCheckIns = RecoveryAnalyticsSupport.loadPriorCheckIns(
				checkInRepository, athleteId, stateDate, baselineWindowDays);

		List<RecoveryMetricDeviationResult> deviations = checkIn
				.map(target -> RecoveryAnalyticsSupport.buildDeviations(
						target, stateDate, baselineWindowDays, priorCheckIns, clock))
				.orElseGet(() -> RecoveryAnalyticsSupport.unavailableMetricComparisons(
						stateDate, baselineWindowDays, priorCheckIns, clock));

		List<DailyAthleteStateRecoveryMetricSnapshot> metrics = deviations.stream()
				.map(DailyAthleteStateSupport::toMetricSnapshot)
				.sorted(Comparator.comparing(m -> m.metricType().name()))
				.toList();

		List<DailyAthleteStateDiscomfortSnapshot> discomfort = checkIn
				.map(DailyAthleteStateSupport::toDiscomfort)
				.orElse(List.of());

		DailyTrainingLoadSummary load = RecoveryAnalyticsSupport.loadTrainingLoadByDate(
						trainingLoadQueryRepository, athleteId, stateDate, stateDate)
				.get(stateDate);

		List<WorkoutOccurrence> occurrences = occurrenceRepository
				.findCalendarRange(athleteId, stateDate, stateDate, null, null)
				.stream()
				.sorted(Comparator
						.comparing((WorkoutOccurrence o) -> o.id().value().toString())
						.thenComparing(WorkoutOccurrence::createdAt))
				.toList();

		ScheduleCounts scheduleCounts = countSchedule(occurrences);
		List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledSnapshots = toScheduled(occurrences);

		List<DailyAthleteStateCategorySummarySnapshot> categories = List.of();
		List<DailyAthleteStateMovementSummarySnapshot> movements = List.of();
		boolean hasTrainingLoad = load != null && load.occurrenceCount() > 0;
		long occurrenceCount = 0;
		long rated = 0;
		long unrated = 0;
		long completedExerciseCount = 0;
		long completedSetCount = 0;
		long completedRepetitionCount = 0;
		BigDecimal totalVolume = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
		long totalDurationSeconds = 0;
		BigDecimal totalDistance = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
		BigDecimal totalSessionRpeLoad = null;
		BigDecimal averageSessionRpe = null;
		long totalSessionDurationMinutes = 0;
		long noImpact = 0;
		long lowImpact = 0;
		long moderateImpact = 0;
		long highImpact = 0;

		if (load != null) {
			occurrenceCount = load.occurrenceCount();
			rated = load.ratedOccurrenceCount();
			unrated = load.unratedOccurrenceCount();
			completedExerciseCount = load.completedExerciseCount();
			completedSetCount = load.completedSetCount();
			completedRepetitionCount = load.completedRepetitionCount();
			totalVolume = load.totalVolumeKilograms();
			totalDurationSeconds = load.totalDurationSeconds();
			totalDistance = load.totalDistanceMeters();
			totalSessionRpeLoad = load.totalSessionRpeLoad();
			averageSessionRpe = load.averageSessionRpe();
			totalSessionDurationMinutes = load.totalSessionDurationMinutes();
			noImpact = load.noImpactExerciseCount();
			lowImpact = load.lowImpactExerciseCount();
			moderateImpact = load.moderateImpactExerciseCount();
			highImpact = load.highImpactExerciseCount();
			categories = load.categorySummaries().stream()
					.sorted(Comparator.comparing(c -> c.category().name()))
					.map(DailyAthleteStateSupport::toCategory)
					.toList();
			movements = load.movementSummaries().stream()
					.sorted(Comparator.comparing(m -> m.primaryMovementPattern().name()))
					.map(DailyAthleteStateSupport::toMovement)
					.toList();
			hasTrainingLoad = occurrenceCount > 0
					|| completedExerciseCount > 0
					|| totalVolume.compareTo(BigDecimal.ZERO) > 0
					|| totalDistance.compareTo(BigDecimal.ZERO) > 0
					|| totalDurationSeconds > 0;
		}

		String trainingCanonical = buildTrainingCanonical(
				occurrences, athleteId, loadSummaryRepository, sessionEffortRepository, load);
		String scheduleCanonical = scheduledSnapshots.stream()
				.map(s -> s.occurrenceId() + ":" + s.occurrenceStatus().name() + ":" + s.orderIndex())
				.sorted()
				.collect(Collectors.joining("|"));
		String recoveryCanonical = checkIn.map(DailyAthleteStateSupport::recoveryCanonical).orElse("ABSENT");
		String priorsCanonical = priorCheckIns.stream()
				.sorted(Comparator.comparing(DailyRecoveryCheckIn::checkInDate)
						.thenComparing(c -> c.id().value().toString()))
				.map(c -> c.id().value() + ":" + c.version() + ":" + c.checkInDate())
				.collect(Collectors.joining("|"));
		String metricsCanonical = metrics.stream()
				.map(m -> m.metricType().name() + ":"
						+ m.observationCount() + ":"
						+ m.baselineMean() + ":"
						+ m.comparisonBand().name() + ":"
						+ m.targetValue() + ":"
						+ m.standardizedDeviation() + ":"
						+ m.dataSufficiency().name())
				.collect(Collectors.joining("|"));
		String discomfortCanonical = discomfort.stream()
				.sorted(DailyAthleteStateFingerprintCalculator.discomfortOrder())
				.map(d -> d.bodyArea().name() + ":" + d.bodySide().name() + ":" + d.intensity() + ":"
						+ nullSafe(d.notes()))
				.collect(Collectors.joining("|"));

		var fingerprintInput = new DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput(
				baselineWindowDays,
				ANALYTICS_VERSION,
				recoveryCanonical,
				priorsCanonical,
				metricsCanonical,
				discomfortCanonical,
				trainingCanonical,
				scheduleCanonical);

		return new AssembledDailyAthleteStateSource(
				checkIn.isPresent(),
				checkIn.map(c -> c.id().value()).orElse(null),
				checkIn.map(DailyRecoveryCheckIn::version).orElse(null),
				checkIn.map(DailyRecoveryCheckIn::sleepDurationMinutes).orElse(null),
				checkIn.map(c -> c.sleepQuality() == null ? null : c.sleepQuality().value()).orElse(null),
				checkIn.map(c -> c.fatigue().value()).orElse(null),
				checkIn.map(c -> c.muscleSoreness().value()).orElse(null),
				checkIn.map(c -> c.stress().value()).orElse(null),
				checkIn.map(c -> c.mood().value()).orElse(null),
				checkIn.map(c -> c.motivation().value()).orElse(null),
				checkIn.map(DailyRecoveryCheckIn::submittedAt).orElse(null),
				checkIn.map(DailyRecoveryCheckIn::updatedAt).orElse(null),
				hasTrainingLoad,
				occurrenceCount,
				scheduleCounts.completed(),
				rated,
				unrated,
				completedExerciseCount,
				completedSetCount,
				completedRepetitionCount,
				totalVolume,
				totalDurationSeconds,
				totalDistance,
				totalSessionRpeLoad,
				averageSessionRpe,
				totalSessionDurationMinutes,
				noImpact,
				lowImpact,
				moderateImpact,
				highImpact,
				scheduleCounts.scheduledOccurrenceCount(),
				scheduleCounts.scheduledWorkoutCount(),
				scheduleCounts.completed(),
				scheduleCounts.skipped(),
				scheduleCounts.cancelled(),
				scheduleCounts.inProgress(),
				metrics,
				discomfort,
				categories,
				movements,
				scheduledSnapshots,
				fingerprintInput);
	}

	private static DailyAthleteStateRecoveryMetricSnapshot toMetricSnapshot(RecoveryMetricDeviationResult deviation) {
		RecoveryMetricBaselineResult baseline = deviation.baseline();
		return new DailyAthleteStateRecoveryMetricSnapshot(
				deviation.metricType(),
				deviation.targetValue(),
				deviation.scaleDirection(),
				baseline.observationCount(),
				baseline.dataSufficiency(),
				baseline.mean(),
				baseline.median(),
				baseline.minimum(),
				baseline.maximum(),
				baseline.standardDeviation(),
				deviation.absoluteDifference(),
				deviation.percentageDifference(),
				deviation.standardizedDeviation(),
				deviation.comparisonBand(),
				deviation.reasonCode());
	}

	private static List<DailyAthleteStateDiscomfortSnapshot> toDiscomfort(DailyRecoveryCheckIn checkIn) {
		List<DailyAthleteStateDiscomfortSnapshot> rows = new ArrayList<>();
		for (BodyAreaDiscomfortObservation observation : checkIn.discomfortAreas()) {
			rows.add(new DailyAthleteStateDiscomfortSnapshot(
					UUID.randomUUID(),
					observation.bodyArea(),
					observation.side(),
					observation.intensity().value(),
					observation.notes(),
					observation.orderIndex()));
		}
		rows.sort(DailyAthleteStateFingerprintCalculator.discomfortOrder());
		return List.copyOf(rows);
	}

	private static DailyAthleteStateCategorySummarySnapshot toCategory(WorkoutLoadCategorySummary summary) {
		return new DailyAthleteStateCategorySummarySnapshot(
				summary.category(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.volumeKilograms(),
				summary.durationSeconds(),
				summary.distanceMeters());
	}

	private static DailyAthleteStateMovementSummarySnapshot toMovement(WorkoutLoadMovementPatternSummary summary) {
		return new DailyAthleteStateMovementSummarySnapshot(
				summary.primaryMovementPattern(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.completedRepetitionCount(),
				summary.volumeKilograms(),
				summary.durationSeconds(),
				summary.distanceMeters());
	}

	private static List<DailyAthleteStateScheduledOccurrenceSnapshot> toScheduled(List<WorkoutOccurrence> occurrences) {
		List<DailyAthleteStateScheduledOccurrenceSnapshot> rows = new ArrayList<>();
		int order = 0;
		for (WorkoutOccurrence occurrence : occurrences) {
			rows.add(new DailyAthleteStateScheduledOccurrenceSnapshot(
					occurrence.id().value(),
					occurrence.trainingPlanId().value(),
					occurrence.workoutDayId().value(),
					occurrence.scheduledDate(),
					occurrence.status(),
					occurrence.plannedEnvironment() == null ? null : occurrence.plannedEnvironment().nameSnapshot(),
					occurrence.actualEnvironment() == null ? null : occurrence.actualEnvironment().nameSnapshot(),
					order++));
		}
		return List.copyOf(rows);
	}

	private static ScheduleCounts countSchedule(List<WorkoutOccurrence> occurrences) {
		long scheduled = 0;
		long completed = 0;
		long skipped = 0;
		long cancelled = 0;
		long inProgress = 0;
		Set<UUID> workoutDays = new HashSet<>();
		for (WorkoutOccurrence occurrence : occurrences) {
			workoutDays.add(occurrence.workoutDayId().value());
			switch (occurrence.status()) {
				case SCHEDULED -> scheduled++;
				case IN_PROGRESS -> inProgress++;
				case COMPLETED -> completed++;
				case SKIPPED -> skipped++;
				case CANCELLED -> cancelled++;
			}
		}
		long scheduledOccurrenceCount = scheduled + inProgress + completed + skipped;
		return new ScheduleCounts(
				scheduledOccurrenceCount,
				workoutDays.size(),
				completed,
				skipped,
				cancelled,
				inProgress);
	}

	private static String recoveryCanonical(DailyRecoveryCheckIn checkIn) {
		return String.join(":",
				checkIn.id().value().toString(),
				String.valueOf(checkIn.version()),
				checkIn.checkInDate().toString(),
				String.valueOf(checkIn.sleepDurationMinutes()),
				String.valueOf(checkIn.sleepQuality() == null ? null : checkIn.sleepQuality().value()),
				String.valueOf(checkIn.fatigue().value()),
				String.valueOf(checkIn.muscleSoreness().value()),
				String.valueOf(checkIn.stress().value()),
				String.valueOf(checkIn.mood().value()),
				String.valueOf(checkIn.motivation().value()));
	}

	private static String buildTrainingCanonical(
			List<WorkoutOccurrence> occurrences,
			AthleteId athleteId,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			DailyTrainingLoadSummary dailyLoad) {
		List<String> lines = new ArrayList<>();
		if (dailyLoad != null) {
			lines.add("daily:"
					+ dailyLoad.occurrenceCount() + ":"
					+ dailyLoad.totalVolumeKilograms() + ":"
					+ dailyLoad.totalDistanceMeters() + ":"
					+ dailyLoad.totalDurationSeconds() + ":"
					+ dailyLoad.totalSessionRpeLoad() + ":"
					+ dailyLoad.averageSessionRpe() + ":"
					+ dailyLoad.completedExerciseCount() + ":"
					+ dailyLoad.completedSetCount() + ":"
					+ dailyLoad.completedRepetitionCount());
		}
		for (WorkoutOccurrence occurrence : occurrences) {
			Optional<WorkoutOccurrenceLoadSummary> summary = loadSummaryRepository
					.findByOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
			Optional<WorkoutSessionEffort> effort = sessionEffortRepository
					.findByOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
			lines.add("occ:"
					+ occurrence.id().value() + ":"
					+ occurrence.status().name() + ":"
					+ occurrence.version() + ":"
					+ summary.map(s -> s.id().value() + ":" + s.calculationVersion().persistenceValue() + ":"
							+ s.updatedAt() + ":" + s.totalVolumeKilograms() + ":" + s.totalDistanceMeters()
							+ ":" + s.totalDurationSeconds() + ":"
							+ (s.sessionRpeLoad() == null ? "null" : s.sessionRpeLoad().value()))
					.orElse("NONE")
					+ ":"
					+ effort.map(e -> e.id().value() + ":" + e.version() + ":" + e.sessionRpe().value())
					.orElse("NO_EFFORT"));
		}
		return lines.stream().sorted().collect(Collectors.joining("|"));
	}

	private static String nullSafe(String value) {
		return value == null ? "" : value;
	}

	private record ScheduleCounts(
			long scheduledOccurrenceCount,
			long scheduledWorkoutCount,
			long completed,
			long skipped,
			long cancelled,
			long inProgress) {
	}

}
