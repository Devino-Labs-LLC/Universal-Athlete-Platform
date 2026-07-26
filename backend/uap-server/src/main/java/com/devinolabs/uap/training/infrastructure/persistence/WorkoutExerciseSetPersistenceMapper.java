package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutExerciseSetPersistenceMapper {

	private WorkoutExerciseSetPersistenceMapper() {
	}

	static WorkoutExerciseSetJpaEntity toEntity(WorkoutExerciseSet set, boolean isNew) {
		return new WorkoutExerciseSetJpaEntity(
				set.id().value(),
				set.workoutExerciseExecutionId().value(),
				set.workoutOccurrenceId().value(),
				set.athleteId().value(),
				set.setNumber(),
				set.displayOrder(),
				set.setType(),
				set.prescribedMinimumReps(),
				set.prescribedMaximumReps(),
				set.prescribedWeight(),
				set.prescribedWeightUnit(),
				set.prescribedDurationSeconds(),
				set.prescribedDistance(),
				set.prescribedDistanceUnit(),
				set.prescribedTargetRpe(),
				set.prescribedRestSeconds(),
				set.actualReps(),
				set.actualWeight(),
				set.actualWeightUnit(),
				set.actualDurationSeconds(),
				set.actualDistance(),
				set.actualDistanceUnit(),
				set.actualRestSeconds(),
				set.actualRpe(),
				set.status(),
				set.startedAt(),
				set.completedAt(),
				set.athleteNotes(),
				set.createdAt(),
				set.updatedAt(),
				set.version(),
				isNew);
	}

	static WorkoutExerciseSet toDomain(WorkoutExerciseSetJpaEntity entity) {
		return WorkoutExerciseSet.rehydrate(
				WorkoutExerciseSetId.of(entity.getId()),
				WorkoutExerciseExecutionId.of(entity.getWorkoutExerciseExecutionId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getSetNumber(),
				entity.getDisplayOrder(),
				entity.getSetType(),
				entity.getPrescribedMinimumReps(),
				entity.getPrescribedMaximumReps(),
				entity.getPrescribedWeight(),
				entity.getPrescribedWeightUnit(),
				entity.getPrescribedDurationSeconds(),
				entity.getPrescribedDistance(),
				entity.getPrescribedDistanceUnit(),
				entity.getPrescribedTargetRpe(),
				entity.getPrescribedRestSeconds(),
				entity.getActualReps(),
				entity.getActualWeight(),
				entity.getActualWeightUnit(),
				entity.getActualDurationSeconds(),
				entity.getActualDistance(),
				entity.getActualDistanceUnit(),
				entity.getActualRestSeconds(),
				entity.getActualRpe(),
				entity.getStatus(),
				entity.getStartedAt(),
				entity.getCompletedAt(),
				entity.getAthleteNotes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
