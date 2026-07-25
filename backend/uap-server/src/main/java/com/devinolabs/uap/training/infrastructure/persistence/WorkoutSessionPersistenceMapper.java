package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutSession;
import com.devinolabs.uap.training.domain.WorkoutSessionId;

final class WorkoutSessionPersistenceMapper {

	private WorkoutSessionPersistenceMapper() {
	}

	static WorkoutSessionJpaEntity toEntity(WorkoutSession session, boolean isNew) {
		return new WorkoutSessionJpaEntity(
				session.id().value(),
				session.workoutExerciseId().value(),
				session.workoutDayId().value(),
				session.athleteId().value(),
				session.status(),
				session.actualSets(),
				session.actualReps(),
				session.actualWeight(),
				session.weightUnit(),
				session.actualDurationSeconds(),
				session.actualDistance(),
				session.distanceUnit(),
				session.actualRestSeconds(),
				session.actualRpe(),
				session.completedAt(),
				session.athleteNotes(),
				session.createdAt(),
				session.updatedAt(),
				session.version(),
				isNew);
	}

	static WorkoutSession toDomain(WorkoutSessionJpaEntity entity) {
		return WorkoutSession.rehydrate(
				WorkoutSessionId.of(entity.getId()),
				WorkoutExerciseId.of(entity.getWorkoutExerciseId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getStatus(),
				entity.getActualSets(),
				entity.getActualReps(),
				entity.getActualWeight(),
				entity.getWeightUnit(),
				entity.getActualDurationSeconds(),
				entity.getActualDistance(),
				entity.getDistanceUnit(),
				entity.getActualRestSeconds(),
				entity.getActualRpe(),
				entity.getCompletedAt(),
				entity.getAthleteNotes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
