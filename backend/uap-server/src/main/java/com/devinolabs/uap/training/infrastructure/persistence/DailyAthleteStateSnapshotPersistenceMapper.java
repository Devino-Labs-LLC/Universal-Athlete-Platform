package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotSummary;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateCategorySummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateDiscomfortSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateMovementSummarySnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateRecoveryMetricSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateScheduledOccurrenceSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;

final class DailyAthleteStateSnapshotPersistenceMapper {

	private DailyAthleteStateSnapshotPersistenceMapper() {
	}

	static DailyAthleteStateSnapshotJpaEntity toNewEntity(DailyAthleteStateSnapshot snapshot) {
		DailyAthleteStateSnapshotJpaEntity entity = DailyAthleteStateSnapshotJpaEntity.createNew(
				snapshot.id().value(),
				snapshot.athleteId().value(),
				snapshot.stateDate(),
				snapshot.snapshotVersion(),
				snapshot.current(),
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
				snapshot.scheduledOccurrenceCount(),
				snapshot.scheduledWorkoutCount(),
				snapshot.completedScheduledCount(),
				snapshot.skippedScheduledCount(),
				snapshot.cancelledScheduledCount(),
				snapshot.inProgressScheduledCount(),
				snapshot.createdAt());

		for (DailyAthleteStateRecoveryMetricSnapshot metric : snapshot.recoveryMetrics()) {
			entity.getRecoveryMetrics().add(DailyAthleteStateRecoveryMetricJpaEntity.of(
					entity,
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
					metric.reasonCode()));
		}
		for (DailyAthleteStateDiscomfortSnapshot discomfort : snapshot.discomfortObservations()) {
			entity.getDiscomfort().add(DailyAthleteStateDiscomfortJpaEntity.of(
					discomfort.id(),
					entity,
					discomfort.bodyArea(),
					discomfort.bodySide(),
					discomfort.intensity(),
					discomfort.notes(),
					discomfort.orderIndex()));
		}
		for (DailyAthleteStateCategorySummarySnapshot category : snapshot.categorySummaries()) {
			entity.getCategories().add(DailyAthleteStateCategorySummaryJpaEntity.of(
					entity,
					category.category(),
					category.completedExerciseCount(),
					category.completedSetCount(),
					category.volumeKilograms(),
					category.durationSeconds(),
					category.distanceMeters()));
		}
		for (DailyAthleteStateMovementSummarySnapshot movement : snapshot.movementSummaries()) {
			entity.getMovements().add(DailyAthleteStateMovementSummaryJpaEntity.of(
					entity,
					movement.movementPattern(),
					movement.completedExerciseCount(),
					movement.completedSetCount(),
					movement.completedRepetitionCount(),
					movement.volumeKilograms(),
					movement.durationSeconds(),
					movement.distanceMeters()));
		}
		for (DailyAthleteStateScheduledOccurrenceSnapshot scheduled : snapshot.scheduledOccurrences()) {
			entity.getScheduledOccurrences().add(DailyAthleteStateScheduledOccurrenceJpaEntity.of(
					entity,
					scheduled.occurrenceId(),
					scheduled.trainingPlanId(),
					scheduled.workoutDayId(),
					scheduled.occurrenceStatus(),
					scheduled.scheduledDate(),
					scheduled.plannedEnvironmentNameSnapshot(),
					scheduled.actualEnvironmentNameSnapshot(),
					scheduled.orderIndex()));
		}
		return entity;
	}

	static DailyAthleteStateSnapshot toDomain(DailyAthleteStateSnapshotJpaEntity entity) {
		List<DailyAthleteStateRecoveryMetricSnapshot> metrics = new ArrayList<>();
		for (DailyAthleteStateRecoveryMetricJpaEntity metric : entity.getRecoveryMetrics()) {
			metrics.add(new DailyAthleteStateRecoveryMetricSnapshot(
					metric.getMetricType(),
					metric.getTargetValue(),
					metric.getMetricDirection(),
					metric.getObservationCount(),
					metric.getDataSufficiency(),
					metric.getBaselineMean(),
					metric.getBaselineMedian(),
					metric.getBaselineMinimum(),
					metric.getBaselineMaximum(),
					metric.getBaselineStandardDeviation(),
					metric.getAbsoluteDifference(),
					metric.getPercentageDifference(),
					metric.getStandardizedDeviation(),
					metric.getComparisonBand(),
					metric.getReasonCode()));
		}
		List<DailyAthleteStateDiscomfortSnapshot> discomfort = new ArrayList<>();
		for (DailyAthleteStateDiscomfortJpaEntity row : entity.getDiscomfort()) {
			discomfort.add(new DailyAthleteStateDiscomfortSnapshot(
					row.getId(),
					row.getBodyArea(),
					row.getBodySide(),
					row.getIntensity(),
					row.getNotes(),
					row.getOrderIndex()));
		}
		List<DailyAthleteStateCategorySummarySnapshot> categories = new ArrayList<>();
		for (DailyAthleteStateCategorySummaryJpaEntity row : entity.getCategories()) {
			categories.add(new DailyAthleteStateCategorySummarySnapshot(
					row.getCategory(),
					row.getCompletedExerciseCount(),
					row.getCompletedSetCount(),
					row.getVolumeKilograms(),
					row.getDurationSeconds(),
					row.getDistanceMeters()));
		}
		List<DailyAthleteStateMovementSummarySnapshot> movements = new ArrayList<>();
		for (DailyAthleteStateMovementSummaryJpaEntity row : entity.getMovements()) {
			movements.add(new DailyAthleteStateMovementSummarySnapshot(
					row.getMovementPattern(),
					row.getCompletedExerciseCount(),
					row.getCompletedSetCount(),
					row.getCompletedRepetitionCount(),
					row.getVolumeKilograms(),
					row.getDurationSeconds(),
					row.getDistanceMeters()));
		}
		List<DailyAthleteStateScheduledOccurrenceSnapshot> scheduled = new ArrayList<>();
		for (DailyAthleteStateScheduledOccurrenceJpaEntity row : entity.getScheduledOccurrences()) {
			scheduled.add(new DailyAthleteStateScheduledOccurrenceSnapshot(
					row.getOccurrenceId(),
					row.getTrainingPlanId(),
					row.getWorkoutDayId(),
					row.getScheduledDate(),
					row.getOccurrenceStatus(),
					row.getPlannedEnvironmentNameSnapshot(),
					row.getActualEnvironmentNameSnapshot(),
					row.getOrderIndex()));
		}
		return DailyAthleteStateSnapshot.rehydrate(
				DailyAthleteStateSnapshotId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getStateDate(),
				entity.getSnapshotVersion(),
				entity.isCurrentSnapshot(),
				entity.getSourceFingerprint(),
				entity.getGenerationReason(),
				entity.getGeneratedAt(),
				entity.getCompleteness(),
				entity.getBaselineWindowDays(),
				entity.getRecoveryAnalyticsCalculationVersion(),
				entity.isCheckInPresent(),
				entity.getRecoveryCheckInId(),
				entity.getRecoveryCheckInVersion(),
				entity.getSleepDurationMinutes(),
				entity.getSleepQuality(),
				entity.getFatigue(),
				entity.getMuscleSoreness(),
				entity.getStress(),
				entity.getMood(),
				entity.getMotivation(),
				entity.getCheckInSubmittedAt(),
				entity.getCheckInLastUpdatedAt(),
				entity.getOccurrenceCount(),
				entity.getCompletedOccurrenceCount(),
				entity.getRatedOccurrenceCount(),
				entity.getUnratedOccurrenceCount(),
				entity.getCompletedExerciseCount(),
				entity.getCompletedSetCount(),
				entity.getCompletedRepetitionCount(),
				entity.getTotalVolumeKilograms(),
				entity.getTotalDurationSeconds(),
				entity.getTotalDistanceMeters(),
				entity.getTotalSessionRpeLoad(),
				entity.getAverageSessionRpe(),
				entity.getTotalSessionDurationMinutes(),
				entity.getNoImpactExerciseCount(),
				entity.getLowImpactExerciseCount(),
				entity.getModerateImpactExerciseCount(),
				entity.getHighImpactExerciseCount(),
				entity.getScheduledOccurrenceCount(),
				entity.getScheduledWorkoutCount(),
				entity.getCompletedScheduledCount(),
				entity.getSkippedScheduledCount(),
				entity.getCancelledScheduledCount(),
				entity.getInProgressScheduledCount(),
				entity.getCreatedAt(),
				metrics,
				discomfort,
				categories,
				movements,
				scheduled);
	}

	static DailyAthleteStateSnapshotSummary toSummary(DailyAthleteStateSnapshotJpaEntity entity) {
		return new DailyAthleteStateSnapshotSummary(
				entity.getId(),
				entity.getStateDate(),
				entity.getSnapshotVersion(),
				entity.isCurrentSnapshot(),
				entity.getGeneratedAt(),
				entity.getGenerationReason(),
				entity.getSourceFingerprint(),
				entity.getRecoveryAnalyticsCalculationVersion(),
				entity.getBaselineWindowDays(),
				entity.getCompleteness());
	}

}
