package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistory;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistoryId;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PerformanceMeasurement;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class AthleteExercisePersonalRecordHistoryPersistenceMapper {

	private AthleteExercisePersonalRecordHistoryPersistenceMapper() {
	}

	static AthleteExercisePersonalRecordHistoryJpaEntity toEntity(
			AthleteExercisePersonalRecordHistory entry,
			boolean isNew) {
		PerformanceMeasurement measurement = entry.measurement();
		return new AthleteExercisePersonalRecordHistoryJpaEntity(
				entry.id().value(),
				entry.personalRecordId().value(),
				entry.athleteId().value(),
				entry.exercisePerformanceKey().value(),
				entry.recordType(),
				entry.recordQualifier(),
				entry.exerciseName(),
				measurement.normalizedValue(),
				measurement.normalizedUnit(),
				measurement.measuredValue(),
				measurement.measuredUnit(),
				measurement.estimated(),
				entry.repetitions(),
				entry.weightValue(),
				entry.weightUnit(),
				entry.achievedAt(),
				entry.scheduledDate(),
				entry.sourceSetId().value(),
				entry.sourceExecutionId().value(),
				entry.sourceOccurrenceId().value(),
				entry.supersededAt(),
				entry.supersededByHistoryId() == null ? null : entry.supersededByHistoryId().value(),
				entry.createdAt(),
				entry.updatedAt(),
				entry.version(),
				isNew);
	}

	static AthleteExercisePersonalRecordHistory toDomain(AthleteExercisePersonalRecordHistoryJpaEntity entity) {
		return AthleteExercisePersonalRecordHistory.rehydrate(
				AthleteExercisePersonalRecordHistoryId.of(entity.getId()),
				AthleteExercisePersonalRecordId.of(entity.getPersonalRecordId()),
				AthleteId.of(entity.getAthleteId()),
				ExercisePerformanceKey.of(entity.getExercisePerformanceKey()),
				entity.getRecordType(),
				entity.getRecordQualifier(),
				entity.getExerciseName(),
				new PerformanceMeasurement(
						entity.getNormalizedValue(),
						entity.getNormalizedUnit(),
						entity.getMeasuredValue(),
						entity.getMeasuredUnit(),
						entity.isEstimated()),
				entity.getRepetitions(),
				entity.getWeightValue(),
				entity.getWeightUnit(),
				entity.getAchievedAt(),
				entity.getScheduledDate(),
				WorkoutExerciseSetId.of(entity.getSourceSetId()),
				WorkoutExerciseExecutionId.of(entity.getSourceExecutionId()),
				WorkoutOccurrenceId.of(entity.getSourceOccurrenceId()),
				entity.getSupersededAt(),
				entity.getSupersededByHistoryId() == null
						? null
						: AthleteExercisePersonalRecordHistoryId.of(entity.getSupersededByHistoryId()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
