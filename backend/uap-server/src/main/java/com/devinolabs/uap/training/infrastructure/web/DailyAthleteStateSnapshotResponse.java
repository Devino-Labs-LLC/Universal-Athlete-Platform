package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotResult;
import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;

record DailyAthleteStateSnapshotResponse(
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
		DailyAthleteStateRecoveryResponse recovery,
		List<DailyAthleteStateRecoveryMetricResponse> recoveryMetrics,
		DailyAthleteStateTrainingLoadResponse trainingLoad,
		DailyAthleteStateScheduleResponse schedule,
		Instant createdAt) {

	static DailyAthleteStateSnapshotResponse from(DailyAthleteStateSnapshotResult result) {
		return new DailyAthleteStateSnapshotResponse(
				result.snapshotId(),
				result.stateDate(),
				result.snapshotVersion(),
				result.current(),
				result.changed(),
				result.sourceFingerprint(),
				result.generationReason(),
				result.generatedAt(),
				result.completeness(),
				result.baselineWindowDays(),
				result.recoveryAnalyticsCalculationVersion(),
				new DailyAthleteStateRecoveryResponse(
						result.checkInPresent(),
						result.recoveryCheckInId(),
						result.recoveryCheckInVersion(),
						result.sleepDurationMinutes(),
						result.sleepQuality(),
						result.fatigue(),
						result.muscleSoreness(),
						result.stress(),
						result.mood(),
						result.motivation(),
						result.checkInSubmittedAt(),
						result.checkInLastUpdatedAt(),
						result.discomfortObservations().stream()
								.map(d -> new DailyAthleteStateDiscomfortResponse(
										d.bodyArea(),
										d.bodySide(),
										d.intensity(),
										d.notes(),
										d.orderIndex()))
								.toList()),
				result.recoveryMetrics().stream()
						.map(DailyAthleteStateRecoveryMetricResponse::from)
						.toList(),
				new DailyAthleteStateTrainingLoadResponse(
						result.occurrenceCount(),
						result.completedOccurrenceCount(),
						result.ratedOccurrenceCount(),
						result.unratedOccurrenceCount(),
						result.completedExerciseCount(),
						result.completedSetCount(),
						result.completedRepetitionCount(),
						result.totalVolumeKilograms(),
						result.totalDurationSeconds(),
						result.totalDistanceMeters(),
						result.totalSessionRpeLoad(),
						result.averageSessionRpe(),
						result.totalSessionDurationMinutes(),
						result.noImpactExerciseCount(),
						result.lowImpactExerciseCount(),
						result.moderateImpactExerciseCount(),
						result.highImpactExerciseCount(),
						result.categorySummaries().stream()
								.map(c -> new DailyAthleteStateCategorySummaryResponse(
										c.category(),
										c.completedExerciseCount(),
										c.completedSetCount(),
										c.volumeKilograms(),
										c.durationSeconds(),
										c.distanceMeters()))
								.toList(),
						result.movementSummaries().stream()
								.map(m -> new DailyAthleteStateMovementSummaryResponse(
										m.movementPattern(),
										m.completedExerciseCount(),
										m.completedSetCount(),
										m.completedRepetitionCount(),
										m.volumeKilograms(),
										m.durationSeconds(),
										m.distanceMeters()))
								.toList()),
				new DailyAthleteStateScheduleResponse(
						result.scheduledOccurrenceCount(),
						result.scheduledWorkoutCount(),
						result.completedScheduledCount(),
						result.skippedScheduledCount(),
						result.cancelledScheduledCount(),
						result.inProgressScheduledCount(),
						result.scheduledOccurrences().stream()
								.map(s -> new DailyAthleteStateScheduledOccurrenceResponse(
										s.occurrenceId(),
										s.trainingPlanId(),
										s.workoutDayId(),
										s.scheduledDate(),
										s.occurrenceStatus(),
										s.plannedEnvironmentNameSnapshot(),
										s.actualEnvironmentNameSnapshot(),
										s.orderIndex()))
								.toList()),
				result.createdAt());
	}

}

record DailyAthleteStateRecoveryResponse(
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
		List<DailyAthleteStateDiscomfortResponse> discomfortObservations) {
}

record DailyAthleteStateDiscomfortResponse(
		com.devinolabs.uap.training.domain.BodyArea bodyArea,
		com.devinolabs.uap.training.domain.BodySide bodySide,
		int intensity,
		String notes,
		int orderIndex) {
}

record DailyAthleteStateRecoveryMetricResponse(
		com.devinolabs.uap.training.domain.RecoveryMetricType metricType,
		BigDecimal targetValue,
		com.devinolabs.uap.training.domain.RecoveryMetricDirection metricDirection,
		int observationCount,
		com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency dataSufficiency,
		BigDecimal baselineMean,
		BigDecimal baselineMedian,
		BigDecimal baselineMinimum,
		BigDecimal baselineMaximum,
		BigDecimal baselineStandardDeviation,
		BigDecimal absoluteDifference,
		BigDecimal percentageDifference,
		BigDecimal standardizedDeviation,
		com.devinolabs.uap.training.domain.RecoveryComparisonBand comparisonBand,
		com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode reasonCode) {

	static DailyAthleteStateRecoveryMetricResponse from(
			com.devinolabs.uap.training.domain.DailyAthleteStateRecoveryMetricSnapshot metric) {
		return new DailyAthleteStateRecoveryMetricResponse(
				metric.metricType(),
				metric.targetValue(),
				metric.metricDirection(),
				metric.observationCount(),
				metric.dataSufficiency(),
				metric.baselineMean(),
				metric.baselineMedian(),
				metric.baselineMinimum(),
				metric.baselineMaximum(),
				metric.baselineStandardDeviation(),
				metric.absoluteDifference(),
				metric.percentageDifference(),
				metric.standardizedDeviation(),
				metric.comparisonBand(),
				metric.reasonCode());
	}

}

record DailyAthleteStateTrainingLoadResponse(
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
		List<DailyAthleteStateCategorySummaryResponse> categorySummaries,
		List<DailyAthleteStateMovementSummaryResponse> movementSummaries) {
}

record DailyAthleteStateCategorySummaryResponse(
		com.devinolabs.uap.training.domain.ExerciseDefinitionCategory category,
		long completedExerciseCount,
		long completedSetCount,
		BigDecimal volumeKilograms,
		long durationSeconds,
		BigDecimal distanceMeters) {
}

record DailyAthleteStateMovementSummaryResponse(
		com.devinolabs.uap.training.domain.MovementPattern movementPattern,
		long completedExerciseCount,
		long completedSetCount,
		long completedRepetitionCount,
		BigDecimal volumeKilograms,
		long durationSeconds,
		BigDecimal distanceMeters) {
}

record DailyAthleteStateScheduleResponse(
		long scheduledOccurrenceCount,
		long scheduledWorkoutCount,
		long completedScheduledCount,
		long skippedScheduledCount,
		long cancelledScheduledCount,
		long inProgressScheduledCount,
		List<DailyAthleteStateScheduledOccurrenceResponse> scheduledOccurrences) {
}

record DailyAthleteStateScheduledOccurrenceResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		LocalDate scheduledDate,
		com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus occurrenceStatus,
		String plannedEnvironmentNameSnapshot,
		String actualEnvironmentNameSnapshot,
		int orderIndex) {
}
