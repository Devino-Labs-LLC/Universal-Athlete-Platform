package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds an immutable daily athlete-state snapshot from assembled factual sources.
 */
public final class DailyAthleteStateSnapshotFactory {

	private static final BigDecimal ZERO_VOLUME = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
	private static final BigDecimal ZERO_DISTANCE = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);

	private DailyAthleteStateSnapshotFactory() {
	}

	public static DailyAthleteStateSnapshot create(
			AthleteId athleteId,
			LocalDate stateDate,
			int snapshotVersion,
			int baselineWindowDays,
			DailyAthleteStateGenerationReason generationReason,
			AssembledDailyAthleteStateSource source,
			Clock clock) {
		Objects.requireNonNull(source, "source must not be null");
		Instant generatedAt = Instant.now(clock);
		DailyAthleteStateCompleteness completeness = DailyAthleteStateCompletenessResolver.resolve(
				source.checkInPresent(),
				source.hasTrainingLoad(),
				!source.scheduledOccurrences().isEmpty());
		String fingerprint = DailyAthleteStateFingerprintCalculator.calculate(source.fingerprintInput());
		return DailyAthleteStateSnapshot.create(
				athleteId,
				stateDate,
				snapshotVersion,
				fingerprint,
				generationReason,
				generatedAt,
				completeness,
				baselineWindowDays,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				source.checkInPresent(),
				source.recoveryCheckInId(),
				source.recoveryCheckInVersion(),
				source.sleepDurationMinutes(),
				source.sleepQuality(),
				source.fatigue(),
				source.muscleSoreness(),
				source.stress(),
				source.mood(),
				source.motivation(),
				source.checkInSubmittedAt(),
				source.checkInLastUpdatedAt(),
				source.occurrenceCount(),
				source.completedOccurrenceCount(),
				source.ratedOccurrenceCount(),
				source.unratedOccurrenceCount(),
				source.completedExerciseCount(),
				source.completedSetCount(),
				source.completedRepetitionCount(),
				nullToZeroVolume(source.totalVolumeKilograms()),
				source.totalDurationSeconds(),
				nullToZeroDistance(source.totalDistanceMeters()),
				source.totalSessionRpeLoad(),
				source.averageSessionRpe(),
				source.totalSessionDurationMinutes(),
				source.noImpactExerciseCount(),
				source.lowImpactExerciseCount(),
				source.moderateImpactExerciseCount(),
				source.highImpactExerciseCount(),
				source.scheduledOccurrenceCount(),
				source.scheduledWorkoutCount(),
				source.completedScheduledCount(),
				source.skippedScheduledCount(),
				source.cancelledScheduledCount(),
				source.inProgressScheduledCount(),
				source.recoveryMetrics(),
				source.discomfortObservations(),
				source.categorySummaries(),
				source.movementSummaries(),
				source.scheduledOccurrences());
	}

	private static BigDecimal nullToZeroVolume(BigDecimal value) {
		return value == null ? ZERO_VOLUME : value;
	}

	private static BigDecimal nullToZeroDistance(BigDecimal value) {
		return value == null ? ZERO_DISTANCE : value;
	}

	/**
	 * Assembled source facts ready for snapshot persistence and fingerprinting.
	 */
	public record AssembledDailyAthleteStateSource(
			boolean checkInPresent,
			UUID recoveryCheckInId,
			Long recoveryCheckInVersion,
			Integer sleepDurationMinutes,
			Integer sleepQuality,
			Integer fatigue,
			Integer muscleSoreness,
			Integer stress,
			Integer mood,
			Integer motivation,
			Instant checkInSubmittedAt,
			Instant checkInLastUpdatedAt,
			boolean hasTrainingLoad,
			long occurrenceCount,
			long completedOccurrenceCount,
			long ratedOccurrenceCount,
			long unratedOccurrenceCount,
			long completedExerciseCount,
			long completedSetCount,
			long completedRepetitionCount,
			BigDecimal totalVolumeKilograms,
			long totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal totalSessionRpeLoad,
			BigDecimal averageSessionRpe,
			long totalSessionDurationMinutes,
			long noImpactExerciseCount,
			long lowImpactExerciseCount,
			long moderateImpactExerciseCount,
			long highImpactExerciseCount,
			long scheduledOccurrenceCount,
			long scheduledWorkoutCount,
			long completedScheduledCount,
			long skippedScheduledCount,
			long cancelledScheduledCount,
			long inProgressScheduledCount,
			List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics,
			List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations,
			List<DailyAthleteStateCategorySummarySnapshot> categorySummaries,
			List<DailyAthleteStateMovementSummarySnapshot> movementSummaries,
			List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences,
			DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput fingerprintInput) {

		public AssembledDailyAthleteStateSource {
			recoveryMetrics = sortedMetrics(recoveryMetrics);
			discomfortObservations = sortedDiscomfort(discomfortObservations);
			categorySummaries = List.copyOf(categorySummaries == null ? List.of() : categorySummaries);
			movementSummaries = List.copyOf(movementSummaries == null ? List.of() : movementSummaries);
			scheduledOccurrences = sortedSchedule(scheduledOccurrences);
			Objects.requireNonNull(fingerprintInput, "fingerprintInput must not be null");
		}

		private static List<DailyAthleteStateRecoveryMetricSnapshot> sortedMetrics(
				List<DailyAthleteStateRecoveryMetricSnapshot> metrics) {
			List<DailyAthleteStateRecoveryMetricSnapshot> copy = new ArrayList<>(
					metrics == null ? List.of() : metrics);
			copy.sort(DailyAthleteStateFingerprintCalculator.metricOrder());
			return List.copyOf(copy);
		}

		private static List<DailyAthleteStateDiscomfortSnapshot> sortedDiscomfort(
				List<DailyAthleteStateDiscomfortSnapshot> discomfort) {
			List<DailyAthleteStateDiscomfortSnapshot> copy = new ArrayList<>(
					discomfort == null ? List.of() : discomfort);
			copy.sort(DailyAthleteStateFingerprintCalculator.discomfortOrder());
			return List.copyOf(copy);
		}

		private static List<DailyAthleteStateScheduledOccurrenceSnapshot> sortedSchedule(
				List<DailyAthleteStateScheduledOccurrenceSnapshot> schedule) {
			List<DailyAthleteStateScheduledOccurrenceSnapshot> copy = new ArrayList<>(
					schedule == null ? List.of() : schedule);
			copy.sort(Comparator
					.comparingInt(DailyAthleteStateScheduledOccurrenceSnapshot::orderIndex)
					.thenComparing(s -> s.occurrenceId().toString()));
			return List.copyOf(copy);
		}
	}

}
