package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable versioned factual daily athlete-state snapshot.
 * Historical rows are never mutated in place after commit.
 */
public final class DailyAthleteStateSnapshot {

	private final DailyAthleteStateSnapshotId id;
	private final AthleteId athleteId;
	private final LocalDate stateDate;
	private final int snapshotVersion;
	private final boolean current;
	private final String sourceFingerprint;
	private final DailyAthleteStateGenerationReason generationReason;
	private final Instant generatedAt;
	private final DailyAthleteStateCompleteness completeness;
	private final int baselineWindowDays;
	private final RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion;

	private final boolean checkInPresent;
	private final UUID recoveryCheckInId;
	private final Long recoveryCheckInVersion;
	private final Integer sleepDurationMinutes;
	private final Integer sleepQuality;
	private final Integer fatigue;
	private final Integer muscleSoreness;
	private final Integer stress;
	private final Integer mood;
	private final Integer motivation;
	private final Instant checkInSubmittedAt;
	private final Instant checkInLastUpdatedAt;

	private final long occurrenceCount;
	private final long completedOccurrenceCount;
	private final long ratedOccurrenceCount;
	private final long unratedOccurrenceCount;
	private final long completedExerciseCount;
	private final long completedSetCount;
	private final long completedRepetitionCount;
	private final BigDecimal totalVolumeKilograms;
	private final long totalDurationSeconds;
	private final BigDecimal totalDistanceMeters;
	private final BigDecimal totalSessionRpeLoad;
	private final BigDecimal averageSessionRpe;
	private final long totalSessionDurationMinutes;
	private final long noImpactExerciseCount;
	private final long lowImpactExerciseCount;
	private final long moderateImpactExerciseCount;
	private final long highImpactExerciseCount;

	private final long scheduledOccurrenceCount;
	private final long scheduledWorkoutCount;
	private final long completedScheduledCount;
	private final long skippedScheduledCount;
	private final long cancelledScheduledCount;
	private final long inProgressScheduledCount;

	private final Instant createdAt;
	private final List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics;
	private final List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations;
	private final List<DailyAthleteStateCategorySummarySnapshot> categorySummaries;
	private final List<DailyAthleteStateMovementSummarySnapshot> movementSummaries;
	private final List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences;

	private DailyAthleteStateSnapshot(
			DailyAthleteStateSnapshotId id,
			AthleteId athleteId,
			LocalDate stateDate,
			int snapshotVersion,
			boolean current,
			String sourceFingerprint,
			DailyAthleteStateGenerationReason generationReason,
			Instant generatedAt,
			DailyAthleteStateCompleteness completeness,
			int baselineWindowDays,
			RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion,
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
			Instant createdAt,
			List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics,
			List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations,
			List<DailyAthleteStateCategorySummarySnapshot> categorySummaries,
			List<DailyAthleteStateMovementSummarySnapshot> movementSummaries,
			List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.stateDate = Objects.requireNonNull(stateDate, "stateDate must not be null");
		if (snapshotVersion < 1) {
			throw new IllegalArgumentException("snapshotVersion must be >= 1");
		}
		this.snapshotVersion = snapshotVersion;
		this.current = current;
		this.sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint must not be null");
		this.generationReason = Objects.requireNonNull(generationReason, "generationReason must not be null");
		this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
		this.completeness = Objects.requireNonNull(completeness, "completeness must not be null");
		this.baselineWindowDays = baselineWindowDays;
		this.recoveryAnalyticsCalculationVersion = Objects.requireNonNull(
				recoveryAnalyticsCalculationVersion, "recoveryAnalyticsCalculationVersion must not be null");
		this.checkInPresent = checkInPresent;
		this.recoveryCheckInId = recoveryCheckInId;
		this.recoveryCheckInVersion = recoveryCheckInVersion;
		this.sleepDurationMinutes = sleepDurationMinutes;
		this.sleepQuality = sleepQuality;
		this.fatigue = fatigue;
		this.muscleSoreness = muscleSoreness;
		this.stress = stress;
		this.mood = mood;
		this.motivation = motivation;
		this.checkInSubmittedAt = checkInSubmittedAt;
		this.checkInLastUpdatedAt = checkInLastUpdatedAt;
		this.occurrenceCount = occurrenceCount;
		this.completedOccurrenceCount = completedOccurrenceCount;
		this.ratedOccurrenceCount = ratedOccurrenceCount;
		this.unratedOccurrenceCount = unratedOccurrenceCount;
		this.completedExerciseCount = completedExerciseCount;
		this.completedSetCount = completedSetCount;
		this.completedRepetitionCount = completedRepetitionCount;
		this.totalVolumeKilograms = Objects.requireNonNull(totalVolumeKilograms, "totalVolumeKilograms must not be null");
		this.totalDurationSeconds = totalDurationSeconds;
		this.totalDistanceMeters = Objects.requireNonNull(totalDistanceMeters, "totalDistanceMeters must not be null");
		this.totalSessionRpeLoad = totalSessionRpeLoad;
		this.averageSessionRpe = averageSessionRpe;
		this.totalSessionDurationMinutes = totalSessionDurationMinutes;
		this.noImpactExerciseCount = noImpactExerciseCount;
		this.lowImpactExerciseCount = lowImpactExerciseCount;
		this.moderateImpactExerciseCount = moderateImpactExerciseCount;
		this.highImpactExerciseCount = highImpactExerciseCount;
		this.scheduledOccurrenceCount = scheduledOccurrenceCount;
		this.scheduledWorkoutCount = scheduledWorkoutCount;
		this.completedScheduledCount = completedScheduledCount;
		this.skippedScheduledCount = skippedScheduledCount;
		this.cancelledScheduledCount = cancelledScheduledCount;
		this.inProgressScheduledCount = inProgressScheduledCount;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.recoveryMetrics = List.copyOf(recoveryMetrics);
		this.discomfortObservations = List.copyOf(discomfortObservations);
		this.categorySummaries = List.copyOf(categorySummaries);
		this.movementSummaries = List.copyOf(movementSummaries);
		this.scheduledOccurrences = List.copyOf(scheduledOccurrences);
	}

	public static DailyAthleteStateSnapshot create(
			AthleteId athleteId,
			LocalDate stateDate,
			int snapshotVersion,
			String sourceFingerprint,
			DailyAthleteStateGenerationReason generationReason,
			Instant generatedAt,
			DailyAthleteStateCompleteness completeness,
			int baselineWindowDays,
			RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion,
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
			List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences) {
		Instant createdAt = generatedAt;
		return new DailyAthleteStateSnapshot(
				DailyAthleteStateSnapshotId.generate(),
				athleteId,
				stateDate,
				snapshotVersion,
				true,
				sourceFingerprint,
				generationReason,
				generatedAt,
				completeness,
				baselineWindowDays,
				recoveryAnalyticsCalculationVersion,
				checkInPresent,
				recoveryCheckInId,
				recoveryCheckInVersion,
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				checkInSubmittedAt,
				checkInLastUpdatedAt,
				occurrenceCount,
				completedOccurrenceCount,
				ratedOccurrenceCount,
				unratedOccurrenceCount,
				completedExerciseCount,
				completedSetCount,
				completedRepetitionCount,
				totalVolumeKilograms,
				totalDurationSeconds,
				totalDistanceMeters,
				totalSessionRpeLoad,
				averageSessionRpe,
				totalSessionDurationMinutes,
				noImpactExerciseCount,
				lowImpactExerciseCount,
				moderateImpactExerciseCount,
				highImpactExerciseCount,
				scheduledOccurrenceCount,
				scheduledWorkoutCount,
				completedScheduledCount,
				skippedScheduledCount,
				cancelledScheduledCount,
				inProgressScheduledCount,
				createdAt,
				recoveryMetrics,
				discomfortObservations,
				categorySummaries,
				movementSummaries,
				scheduledOccurrences);
	}

	public static DailyAthleteStateSnapshot rehydrate(
			DailyAthleteStateSnapshotId id,
			AthleteId athleteId,
			LocalDate stateDate,
			int snapshotVersion,
			boolean current,
			String sourceFingerprint,
			DailyAthleteStateGenerationReason generationReason,
			Instant generatedAt,
			DailyAthleteStateCompleteness completeness,
			int baselineWindowDays,
			RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion,
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
			Instant createdAt,
			List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics,
			List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations,
			List<DailyAthleteStateCategorySummarySnapshot> categorySummaries,
			List<DailyAthleteStateMovementSummarySnapshot> movementSummaries,
			List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences) {
		return new DailyAthleteStateSnapshot(
				id,
				athleteId,
				stateDate,
				snapshotVersion,
				current,
				sourceFingerprint,
				generationReason,
				generatedAt,
				completeness,
				baselineWindowDays,
				recoveryAnalyticsCalculationVersion,
				checkInPresent,
				recoveryCheckInId,
				recoveryCheckInVersion,
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				checkInSubmittedAt,
				checkInLastUpdatedAt,
				occurrenceCount,
				completedOccurrenceCount,
				ratedOccurrenceCount,
				unratedOccurrenceCount,
				completedExerciseCount,
				completedSetCount,
				completedRepetitionCount,
				totalVolumeKilograms,
				totalDurationSeconds,
				totalDistanceMeters,
				totalSessionRpeLoad,
				averageSessionRpe,
				totalSessionDurationMinutes,
				noImpactExerciseCount,
				lowImpactExerciseCount,
				moderateImpactExerciseCount,
				highImpactExerciseCount,
				scheduledOccurrenceCount,
				scheduledWorkoutCount,
				completedScheduledCount,
				skippedScheduledCount,
				cancelledScheduledCount,
				inProgressScheduledCount,
				createdAt,
				recoveryMetrics,
				discomfortObservations,
				categorySummaries,
				movementSummaries,
				scheduledOccurrences);
	}

	public DailyAthleteStateSnapshot markNotCurrent() {
		if (!current) {
			return this;
		}
		return rehydrate(
				id,
				athleteId,
				stateDate,
				snapshotVersion,
				false,
				sourceFingerprint,
				generationReason,
				generatedAt,
				completeness,
				baselineWindowDays,
				recoveryAnalyticsCalculationVersion,
				checkInPresent,
				recoveryCheckInId,
				recoveryCheckInVersion,
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				checkInSubmittedAt,
				checkInLastUpdatedAt,
				occurrenceCount,
				completedOccurrenceCount,
				ratedOccurrenceCount,
				unratedOccurrenceCount,
				completedExerciseCount,
				completedSetCount,
				completedRepetitionCount,
				totalVolumeKilograms,
				totalDurationSeconds,
				totalDistanceMeters,
				totalSessionRpeLoad,
				averageSessionRpe,
				totalSessionDurationMinutes,
				noImpactExerciseCount,
				lowImpactExerciseCount,
				moderateImpactExerciseCount,
				highImpactExerciseCount,
				scheduledOccurrenceCount,
				scheduledWorkoutCount,
				completedScheduledCount,
				skippedScheduledCount,
				cancelledScheduledCount,
				inProgressScheduledCount,
				createdAt,
				recoveryMetrics,
				discomfortObservations,
				categorySummaries,
				movementSummaries,
				scheduledOccurrences);
	}

	public DailyAthleteStateSnapshotId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public LocalDate stateDate() {
		return stateDate;
	}

	public int snapshotVersion() {
		return snapshotVersion;
	}

	public boolean current() {
		return current;
	}

	public String sourceFingerprint() {
		return sourceFingerprint;
	}

	public DailyAthleteStateGenerationReason generationReason() {
		return generationReason;
	}

	public Instant generatedAt() {
		return generatedAt;
	}

	public DailyAthleteStateCompleteness completeness() {
		return completeness;
	}

	public int baselineWindowDays() {
		return baselineWindowDays;
	}

	public RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion() {
		return recoveryAnalyticsCalculationVersion;
	}

	public boolean checkInPresent() {
		return checkInPresent;
	}

	public UUID recoveryCheckInId() {
		return recoveryCheckInId;
	}

	public Long recoveryCheckInVersion() {
		return recoveryCheckInVersion;
	}

	public Integer sleepDurationMinutes() {
		return sleepDurationMinutes;
	}

	public Integer sleepQuality() {
		return sleepQuality;
	}

	public Integer fatigue() {
		return fatigue;
	}

	public Integer muscleSoreness() {
		return muscleSoreness;
	}

	public Integer stress() {
		return stress;
	}

	public Integer mood() {
		return mood;
	}

	public Integer motivation() {
		return motivation;
	}

	public Instant checkInSubmittedAt() {
		return checkInSubmittedAt;
	}

	public Instant checkInLastUpdatedAt() {
		return checkInLastUpdatedAt;
	}

	public long occurrenceCount() {
		return occurrenceCount;
	}

	public long completedOccurrenceCount() {
		return completedOccurrenceCount;
	}

	public long ratedOccurrenceCount() {
		return ratedOccurrenceCount;
	}

	public long unratedOccurrenceCount() {
		return unratedOccurrenceCount;
	}

	public long completedExerciseCount() {
		return completedExerciseCount;
	}

	public long completedSetCount() {
		return completedSetCount;
	}

	public long completedRepetitionCount() {
		return completedRepetitionCount;
	}

	public BigDecimal totalVolumeKilograms() {
		return totalVolumeKilograms;
	}

	public long totalDurationSeconds() {
		return totalDurationSeconds;
	}

	public BigDecimal totalDistanceMeters() {
		return totalDistanceMeters;
	}

	public BigDecimal totalSessionRpeLoad() {
		return totalSessionRpeLoad;
	}

	public BigDecimal averageSessionRpe() {
		return averageSessionRpe;
	}

	public long totalSessionDurationMinutes() {
		return totalSessionDurationMinutes;
	}

	public long noImpactExerciseCount() {
		return noImpactExerciseCount;
	}

	public long lowImpactExerciseCount() {
		return lowImpactExerciseCount;
	}

	public long moderateImpactExerciseCount() {
		return moderateImpactExerciseCount;
	}

	public long highImpactExerciseCount() {
		return highImpactExerciseCount;
	}

	public long scheduledOccurrenceCount() {
		return scheduledOccurrenceCount;
	}

	public long scheduledWorkoutCount() {
		return scheduledWorkoutCount;
	}

	public long completedScheduledCount() {
		return completedScheduledCount;
	}

	public long skippedScheduledCount() {
		return skippedScheduledCount;
	}

	public long cancelledScheduledCount() {
		return cancelledScheduledCount;
	}

	public long inProgressScheduledCount() {
		return inProgressScheduledCount;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics() {
		return recoveryMetrics;
	}

	public List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations() {
		return discomfortObservations;
	}

	public List<DailyAthleteStateCategorySummarySnapshot> categorySummaries() {
		return categorySummaries;
	}

	public List<DailyAthleteStateMovementSummarySnapshot> movementSummaries() {
		return movementSummaries;
	}

	public List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences() {
		return scheduledOccurrences;
	}

}
