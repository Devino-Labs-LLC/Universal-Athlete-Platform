package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PerformanceMeasurement;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class AthleteExercisePersonalRecordPersistenceMapper {

	private AthleteExercisePersonalRecordPersistenceMapper() {
	}

	static AthleteExercisePersonalRecordJpaEntity toEntity(AthleteExercisePersonalRecord record, boolean isNew) {
		PerformanceMeasurement measurement = record.measurement();
		return new AthleteExercisePersonalRecordJpaEntity(
				record.id().value(),
				record.athleteId().value(),
				record.exercisePerformanceKey().value(),
				record.recordType(),
				record.recordQualifier(),
				record.exerciseName(),
				measurement.normalizedValue(),
				measurement.normalizedUnit(),
				measurement.measuredValue(),
				measurement.measuredUnit(),
				measurement.estimated(),
				record.repetitions(),
				record.weightValue(),
				record.weightUnit(),
				record.achievedAt(),
				record.scheduledDate(),
				record.sourceSetId().value(),
				record.sourceExecutionId().value(),
				record.sourceOccurrenceId().value(),
				record.createdAt(),
				record.updatedAt(),
				record.version(),
				isNew);
	}

	static AthleteExercisePersonalRecord toDomain(AthleteExercisePersonalRecordJpaEntity entity) {
		return AthleteExercisePersonalRecord.rehydrate(
				AthleteExercisePersonalRecordId.of(entity.getId()),
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
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
