package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DailyAthleteStateCategorySummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateDiscomfortSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.DailyAthleteStateMovementSummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateRecoveryMetricSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateScheduledOccurrenceSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;

public record DailyAthleteStateSnapshotResult(
		UUID snapshotId,
		LocalDate stateDate,
		int snapshotVersion,
		boolean current,
		boolean changed,
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
		List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations,
		List<DailyAthleteStateRecoveryMetricSnapshot> recoveryMetrics,
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
		List<DailyAthleteStateCategorySummarySnapshot> categorySummaries,
		List<DailyAthleteStateMovementSummarySnapshot> movementSummaries,
		long scheduledOccurrenceCount,
		long scheduledWorkoutCount,
		long completedScheduledCount,
		long skippedScheduledCount,
		long cancelledScheduledCount,
		long inProgressScheduledCount,
		List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduledOccurrences,
		Instant createdAt) {

	public static DailyAthleteStateSnapshotResult from(DailyAthleteStateSnapshot snapshot, boolean changed) {
		return new DailyAthleteStateSnapshotResult(
				snapshot.id().value(),
				snapshot.stateDate(),
				snapshot.snapshotVersion(),
				snapshot.current(),
				changed,
				snapshot.sourceFingerprint(),
				snapshot.generationReason(),
				snapshot.generatedAt(),
				snapshot.completeness(),
				snapshot.baselineWindowDays(),
				snapshot.recoveryAnalyticsCalculationVersion(),
				snapshot.checkInPresent(),
				snapshot.recoveryCheckInId(),
				snapshot.recoveryCheckInVersion(),
				snapshot.sleepDurationMinutes(),
				snapshot.sleepQuality(),
				snapshot.fatigue(),
				snapshot.muscleSoreness(),
				snapshot.stress(),
				snapshot.mood(),
				snapshot.motivation(),
				snapshot.checkInSubmittedAt(),
				snapshot.checkInLastUpdatedAt(),
				snapshot.discomfortObservations(),
				snapshot.recoveryMetrics(),
				snapshot.occurrenceCount(),
				snapshot.completedOccurrenceCount(),
				snapshot.ratedOccurrenceCount(),
				snapshot.unratedOccurrenceCount(),
				snapshot.completedExerciseCount(),
				snapshot.completedSetCount(),
				snapshot.completedRepetitionCount(),
				snapshot.totalVolumeKilograms(),
				snapshot.totalDurationSeconds(),
				snapshot.totalDistanceMeters(),
				snapshot.totalSessionRpeLoad(),
				snapshot.averageSessionRpe(),
				snapshot.totalSessionDurationMinutes(),
				snapshot.noImpactExerciseCount(),
				snapshot.lowImpactExerciseCount(),
				snapshot.moderateImpactExerciseCount(),
				snapshot.highImpactExerciseCount(),
				snapshot.categorySummaries(),
				snapshot.movementSummaries(),
				snapshot.scheduledOccurrenceCount(),
				snapshot.scheduledWorkoutCount(),
				snapshot.completedScheduledCount(),
				snapshot.skippedScheduledCount(),
				snapshot.cancelledScheduledCount(),
				snapshot.inProgressScheduledCount(),
				snapshot.scheduledOccurrences(),
				snapshot.createdAt());
	}

}
