package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutOccurrencePersistenceMapper {

	private WorkoutOccurrencePersistenceMapper() {
	}

	static WorkoutOccurrenceJpaEntity toEntity(WorkoutOccurrence occurrence, boolean isNew) {
		return new WorkoutOccurrenceJpaEntity(
				occurrence.id().value(),
				occurrence.trainingPlanId().value(),
				occurrence.workoutDayId().value(),
				occurrence.athleteId().value(),
				occurrence.scheduledDate(),
				occurrence.plannedStartTime(),
				occurrence.startedAt(),
				occurrence.completedAt(),
				occurrence.status(),
				occurrence.athleteNotes(),
				occurrence.createdAt(),
				occurrence.updatedAt(),
				occurrence.version(),
				isNew);
	}

	static WorkoutOccurrence toDomain(WorkoutOccurrenceJpaEntity entity) {
		return WorkoutOccurrence.rehydrate(
				WorkoutOccurrenceId.of(entity.getId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getScheduledDate(),
				entity.getPlannedStartTime(),
				entity.getStartedAt(),
				entity.getCompletedAt(),
				entity.getStatus(),
				entity.getAthleteNotes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
