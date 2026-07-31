package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;

final class WorkoutSessionEffortPersistenceMapper {

	private WorkoutSessionEffortPersistenceMapper() {
	}

	static WorkoutSessionEffortJpaEntity toEntity(WorkoutSessionEffort effort, boolean isNew) {
		return WorkoutSessionEffortJpaEntity.fromDomain(effort, isNew);
	}

	static WorkoutSessionEffort toDomain(WorkoutSessionEffortJpaEntity entity) {
		return WorkoutSessionEffort.rehydrate(
				WorkoutSessionEffortId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				SessionRpe.of(entity.getSessionRpe()),
				entity.getSessionDurationMinutes(),
				entity.getDurationSource(),
				entity.getPerceivedNotes(),
				entity.getSubmittedAt(),
				entity.getEffortSource(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	static void applyMutableFields(WorkoutSessionEffortJpaEntity entity, WorkoutSessionEffort effort) {
		entity.setSessionRpe(effort.sessionRpe().value());
		entity.setSessionDurationMinutes(effort.sessionDurationMinutes());
		entity.setDurationSource(effort.durationSource());
		entity.setPerceivedNotes(effort.perceivedNotes());
		entity.setSubmittedAt(effort.submittedAt());
		entity.setUpdatedAt(effort.updatedAt());
	}

}
