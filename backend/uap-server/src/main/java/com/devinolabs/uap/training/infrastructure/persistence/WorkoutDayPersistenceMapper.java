package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;

final class WorkoutDayPersistenceMapper {

	private WorkoutDayPersistenceMapper() {
	}

	static WorkoutDayJpaEntity toEntity(WorkoutDay day, boolean isNew) {
		return new WorkoutDayJpaEntity(
				day.id().value(),
				day.trainingPlanId().value(),
				day.athleteId().value(),
				day.trainingEnvironmentOverrideId() == null
						? null
						: day.trainingEnvironmentOverrideId().value(),
				day.displayOrder(),
				day.title(),
				day.normalizedTitle(),
				day.description(),
				day.planWeekNumber(),
				day.scheduledDayOfWeek(),
				day.plannedStartTime(),
				day.expectedDurationMinutes(),
				day.status(),
				day.createdAt(),
				day.updatedAt(),
				day.version(),
				isNew);
	}

	static WorkoutDay toDomain(WorkoutDayJpaEntity entity) {
		return WorkoutDay.rehydrate(
				WorkoutDayId.of(entity.getId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getTrainingEnvironmentOverrideId() == null
						? null
						: TrainingEnvironmentId.of(entity.getTrainingEnvironmentOverrideId()),
				entity.getDisplayOrder(),
				entity.getTitle(),
				entity.getNormalizedTitle(),
				entity.getDescription(),
				entity.getPlanWeekNumber(),
				entity.getScheduledDayOfWeek(),
				entity.getPlannedStartTime(),
				entity.getExpectedDurationMinutes(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
