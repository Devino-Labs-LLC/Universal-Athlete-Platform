package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.SessionRpeLoad;
import com.devinolabs.uap.training.domain.TrainingLoadCalculationVersion;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutLoadCalculator;
import com.devinolabs.uap.training.domain.WorkoutLoadCategorySummary;
import com.devinolabs.uap.training.domain.WorkoutLoadMovementPatternSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummaryId;

final class WorkoutOccurrenceLoadSummaryPersistenceMapper {

	private WorkoutOccurrenceLoadSummaryPersistenceMapper() {
	}

	static WorkoutOccurrenceLoadSummaryJpaEntity toEntity(WorkoutOccurrenceLoadSummary summary) {
		WorkoutOccurrenceLoadSummaryJpaEntity entity = new WorkoutOccurrenceLoadSummaryJpaEntity();
		applySummaryFields(entity, summary);
		entity.setCategories(toCategoryEntities(summary.categorySummaries(), entity));
		entity.setMovements(toMovementEntities(summary.movementSummaries(), entity));
		entity.setNew(true);
		return entity;
	}

	static WorkoutOccurrenceLoadSummaryJpaEntity toEntity(
			WorkoutOccurrenceLoadSummary summary,
			WorkoutOccurrenceLoadSummaryJpaEntity existing) {
		applySummaryFields(existing, summary);
		syncCategories(existing, summary.categorySummaries());
		syncMovements(existing, summary.movementSummaries());
		return existing;
	}

	static WorkoutOccurrenceLoadSummary toDomain(WorkoutOccurrenceLoadSummaryJpaEntity entity) {
		List<WorkoutLoadCategorySummary> categories = entity.getCategories().stream()
				.map(WorkoutOccurrenceLoadSummaryPersistenceMapper::toCategoryDomain)
				.toList();
		List<WorkoutLoadMovementPatternSummary> movements = entity.getMovements().stream()
				.map(WorkoutOccurrenceLoadSummaryPersistenceMapper::toMovementDomain)
				.toList();
		return toDomain(entity, categories, movements);
	}

	/**
	 * Maps occurrence load headers without touching lazy category/movement collections.
	 */
	static WorkoutOccurrenceLoadSummary toDomainHeaderOnly(WorkoutOccurrenceLoadSummaryJpaEntity entity) {
		return toDomain(entity, List.of(), List.of());
	}

	private static WorkoutOccurrenceLoadSummary toDomain(
			WorkoutOccurrenceLoadSummaryJpaEntity entity,
			List<WorkoutLoadCategorySummary> categories,
			List<WorkoutLoadMovementPatternSummary> movements) {
		WorkoutLoadCalculator.Result result = new WorkoutLoadCalculator.Result(
				entity.getSessionRpe() == null ? null : SessionRpe.of(entity.getSessionRpe()),
				entity.getSessionDurationMinutes(),
				entity.getSessionRpeLoad() == null
						? null
						: SessionRpeLoad.fromPersistence(entity.getSessionRpeLoad()),
				entity.getPrescribedExerciseCount(),
				entity.getCompletedExerciseCount(),
				entity.getSubstitutedExerciseCount(),
				entity.getCompletedSetCount(),
				entity.getSkippedSetCount(),
				entity.getCompletedRepetitionCount(),
				entity.getTotalVolumeKilograms(),
				entity.getTotalDurationSeconds(),
				entity.getTotalDistanceMeters(),
				entity.getNoImpactExerciseCount(),
				entity.getLowImpactExerciseCount(),
				entity.getModerateImpactExerciseCount(),
				entity.getHighImpactExerciseCount(),
				categories,
				movements,
				TrainingLoadCalculationVersion.fromPersistence(entity.getCalculationVersion()));
		return WorkoutOccurrenceLoadSummary.rehydrate(
				WorkoutOccurrenceLoadSummaryId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				entity.getScheduledDate(),
				result,
				entity.getCalculatedAt(),
				entity.getSourceUpdatedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	private static void applySummaryFields(
			WorkoutOccurrenceLoadSummaryJpaEntity entity,
			WorkoutOccurrenceLoadSummary summary) {
		entity.setId(summary.id().value());
		entity.setAthleteId(summary.athleteId().value());
		entity.setTrainingPlanId(summary.trainingPlanId().value());
		entity.setWorkoutDayId(summary.workoutDayId().value());
		entity.setWorkoutOccurrenceId(summary.workoutOccurrenceId().value());
		entity.setScheduledDate(summary.scheduledDate());
		entity.setSessionRpe(summary.sessionRpe() == null ? null : summary.sessionRpe().value());
		entity.setSessionDurationMinutes(summary.sessionDurationMinutes());
		entity.setSessionRpeLoad(summary.sessionRpeLoad() == null ? null : summary.sessionRpeLoad().value());
		entity.setPrescribedExerciseCount(summary.prescribedExerciseCount());
		entity.setCompletedExerciseCount(summary.completedExerciseCount());
		entity.setSubstitutedExerciseCount(summary.substitutedExerciseCount());
		entity.setCompletedSetCount(summary.completedSetCount());
		entity.setSkippedSetCount(summary.skippedSetCount());
		entity.setCompletedRepetitionCount(summary.completedRepetitionCount());
		entity.setTotalVolumeKilograms(summary.totalVolumeKilograms());
		entity.setTotalDurationSeconds(summary.totalDurationSeconds());
		entity.setTotalDistanceMeters(summary.totalDistanceMeters());
		entity.setNoImpactExerciseCount(summary.noImpactExerciseCount());
		entity.setLowImpactExerciseCount(summary.lowImpactExerciseCount());
		entity.setModerateImpactExerciseCount(summary.moderateImpactExerciseCount());
		entity.setHighImpactExerciseCount(summary.highImpactExerciseCount());
		entity.setCalculatedAt(summary.calculatedAt());
		entity.setSourceUpdatedAt(summary.sourceUpdatedAt());
		entity.setCalculationVersion(summary.calculationVersion().persistenceValue());
		entity.setCreatedAt(summary.createdAt());
		entity.setUpdatedAt(summary.updatedAt());
		entity.setVersion(summary.version());
	}

	private static List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> toCategoryEntities(
			List<WorkoutLoadCategorySummary> summaries,
			WorkoutOccurrenceLoadSummaryJpaEntity parent) {
		List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> entities = new ArrayList<>(summaries.size());
		for (WorkoutLoadCategorySummary summary : summaries) {
			entities.add(toCategoryEntity(summary, parent));
		}
		return entities;
	}

	private static List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> toMovementEntities(
			List<WorkoutLoadMovementPatternSummary> summaries,
			WorkoutOccurrenceLoadSummaryJpaEntity parent) {
		List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> entities = new ArrayList<>(summaries.size());
		for (WorkoutLoadMovementPatternSummary summary : summaries) {
			entities.add(toMovementEntity(summary, parent));
		}
		return entities;
	}

	private static void syncCategories(
			WorkoutOccurrenceLoadSummaryJpaEntity parent,
			List<WorkoutLoadCategorySummary> summaries) {
		Map<String, WorkoutOccurrenceLoadCategorySummaryJpaEntity> existingByKey = new LinkedHashMap<>();
		for (WorkoutOccurrenceLoadCategorySummaryJpaEntity existing : parent.getCategories()) {
			existingByKey.put(existing.getCategory().name(), existing);
		}
		List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> synced = new ArrayList<>(summaries.size());
		for (WorkoutLoadCategorySummary summary : summaries) {
			WorkoutOccurrenceLoadCategorySummaryJpaEntity entity =
					existingByKey.remove(summary.category().name());
			if (entity == null) {
				entity = toCategoryEntity(summary, parent);
			}
			else {
				applyCategoryFields(entity, summary);
			}
			synced.add(entity);
		}
		parent.getCategories().clear();
		parent.getCategories().addAll(synced);
	}

	private static void syncMovements(
			WorkoutOccurrenceLoadSummaryJpaEntity parent,
			List<WorkoutLoadMovementPatternSummary> summaries) {
		Map<String, WorkoutOccurrenceLoadMovementSummaryJpaEntity> existingByKey = new LinkedHashMap<>();
		for (WorkoutOccurrenceLoadMovementSummaryJpaEntity existing : parent.getMovements()) {
			existingByKey.put(existing.getPrimaryMovementPattern().name(), existing);
		}
		List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> synced = new ArrayList<>(summaries.size());
		for (WorkoutLoadMovementPatternSummary summary : summaries) {
			WorkoutOccurrenceLoadMovementSummaryJpaEntity entity =
					existingByKey.remove(summary.primaryMovementPattern().name());
			if (entity == null) {
				entity = toMovementEntity(summary, parent);
			}
			else {
				applyMovementFields(entity, summary);
			}
			synced.add(entity);
		}
		parent.getMovements().clear();
		parent.getMovements().addAll(synced);
	}

	private static WorkoutOccurrenceLoadCategorySummaryJpaEntity toCategoryEntity(
			WorkoutLoadCategorySummary summary,
			WorkoutOccurrenceLoadSummaryJpaEntity parent) {
		WorkoutOccurrenceLoadCategorySummaryJpaEntity entity = new WorkoutOccurrenceLoadCategorySummaryJpaEntity();
		entity.setSummary(parent);
		entity.setId(new WorkoutOccurrenceLoadCategorySummaryId(parent.getId(), summary.category()));
		applyCategoryFields(entity, summary);
		return entity;
	}

	private static WorkoutOccurrenceLoadMovementSummaryJpaEntity toMovementEntity(
			WorkoutLoadMovementPatternSummary summary,
			WorkoutOccurrenceLoadSummaryJpaEntity parent) {
		WorkoutOccurrenceLoadMovementSummaryJpaEntity entity = new WorkoutOccurrenceLoadMovementSummaryJpaEntity();
		entity.setSummary(parent);
		entity.setId(new WorkoutOccurrenceLoadMovementSummaryId(parent.getId(), summary.primaryMovementPattern()));
		applyMovementFields(entity, summary);
		return entity;
	}

	private static void applyCategoryFields(
			WorkoutOccurrenceLoadCategorySummaryJpaEntity entity,
			WorkoutLoadCategorySummary summary) {
		entity.setCompletedExerciseCount(summary.completedExerciseCount());
		entity.setCompletedSetCount(summary.completedSetCount());
		entity.setVolumeKilograms(summary.volumeKilograms());
		entity.setDurationSeconds(summary.durationSeconds());
		entity.setDistanceMeters(summary.distanceMeters());
	}

	private static void applyMovementFields(
			WorkoutOccurrenceLoadMovementSummaryJpaEntity entity,
			WorkoutLoadMovementPatternSummary summary) {
		entity.setCompletedExerciseCount(summary.completedExerciseCount());
		entity.setCompletedSetCount(summary.completedSetCount());
		entity.setCompletedRepetitionCount(summary.completedRepetitionCount());
		entity.setVolumeKilograms(summary.volumeKilograms());
		entity.setDurationSeconds(summary.durationSeconds());
		entity.setDistanceMeters(summary.distanceMeters());
	}

	private static WorkoutLoadCategorySummary toCategoryDomain(WorkoutOccurrenceLoadCategorySummaryJpaEntity entity) {
		return new WorkoutLoadCategorySummary(
				entity.getCategory(),
				entity.getCompletedExerciseCount(),
				entity.getCompletedSetCount(),
				entity.getVolumeKilograms(),
				entity.getDurationSeconds(),
				entity.getDistanceMeters());
	}

	private static WorkoutLoadMovementPatternSummary toMovementDomain(
			WorkoutOccurrenceLoadMovementSummaryJpaEntity entity) {
		return new WorkoutLoadMovementPatternSummary(
				entity.getPrimaryMovementPattern(),
				entity.getCompletedExerciseCount(),
				entity.getCompletedSetCount(),
				entity.getCompletedRepetitionCount(),
				entity.getVolumeKilograms(),
				entity.getDurationSeconds(),
				entity.getDistanceMeters());
	}

}
