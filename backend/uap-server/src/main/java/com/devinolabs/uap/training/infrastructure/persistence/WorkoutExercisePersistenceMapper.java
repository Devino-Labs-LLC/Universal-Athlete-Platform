package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

final class WorkoutExercisePersistenceMapper {

	private WorkoutExercisePersistenceMapper() {
	}

	static WorkoutExerciseJpaEntity toEntity(WorkoutExercise exercise, boolean isNew) {
		return new WorkoutExerciseJpaEntity(
				exercise.id().value(),
				exercise.workoutDayId().value(),
				exercise.athleteId().value(),
				exercise.exerciseDefinitionId().value(),
				exercise.displayOrder(),
				exercise.exerciseName(),
				exercise.normalizedExerciseName(),
				exercise.category(),
				exercise.type(),
				exercise.sets(),
				exercise.minimumReps(),
				exercise.maximumReps(),
				exercise.targetWeight(),
				exercise.weightUnit(),
				exercise.targetDurationSeconds(),
				exercise.targetDistance(),
				exercise.distanceUnit(),
				exercise.targetRestSeconds(),
				exercise.targetRpe(),
				exercise.tempo(),
				exercise.coachingNotes(),
				exercise.status(),
				exercise.createdAt(),
				exercise.updatedAt(),
				exercise.version(),
				isNew);
	}

	static WorkoutExercise toDomain(WorkoutExerciseJpaEntity entity) {
		return WorkoutExercise.rehydrate(
				WorkoutExerciseId.of(entity.getId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				AthleteId.of(entity.getAthleteId()),
				ExerciseDefinitionId.of(entity.getExerciseDefinitionId()),
				entity.getDisplayOrder(),
				entity.getExerciseName(),
				entity.getNormalizedExerciseName(),
				entity.getCategory(),
				entity.getType(),
				entity.getSets(),
				entity.getMinimumReps(),
				entity.getMaximumReps(),
				entity.getTargetWeight(),
				entity.getWeightUnit(),
				entity.getTargetDurationSeconds(),
				entity.getTargetDistance(),
				entity.getDistanceUnit(),
				entity.getTargetRestSeconds(),
				entity.getTargetRpe(),
				entity.getTempo(),
				entity.getCoachingNotes(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
