package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevision;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevisionId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;

final class WorkoutSessionEffortRevisionPersistenceMapper {

	private WorkoutSessionEffortRevisionPersistenceMapper() {
	}

	static WorkoutSessionEffortRevisionJpaEntity toEntity(WorkoutSessionEffortRevision revision) {
		WorkoutSessionEffortRevisionJpaEntity entity = new WorkoutSessionEffortRevisionJpaEntity();
		entity.setId(revision.id().value());
		entity.setWorkoutSessionEffortId(revision.workoutSessionEffortId().value());
		entity.setAthleteId(revision.athleteId().value());
		entity.setRevisionNumber(revision.revisionNumber());
		entity.setPriorSessionRpe(revision.priorSessionRpe().value());
		entity.setNewSessionRpe(revision.newSessionRpe().value());
		entity.setPriorDurationMinutes(revision.priorDurationMinutes());
		entity.setNewDurationMinutes(revision.newDurationMinutes());
		entity.setPriorNotes(revision.priorNotes());
		entity.setNewNotes(revision.newNotes());
		entity.setChangedAt(revision.changedAt());
		entity.setCreatedAt(revision.createdAt());
		entity.setNew(true);
		return entity;
	}

	static WorkoutSessionEffortRevision toDomain(WorkoutSessionEffortRevisionJpaEntity entity) {
		return WorkoutSessionEffortRevision.rehydrate(
				WorkoutSessionEffortRevisionId.of(entity.getId()),
				WorkoutSessionEffortId.of(entity.getWorkoutSessionEffortId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getRevisionNumber(),
				SessionRpe.of(entity.getPriorSessionRpe()),
				SessionRpe.of(entity.getNewSessionRpe()),
				entity.getPriorDurationMinutes(),
				entity.getNewDurationMinutes(),
				entity.getPriorNotes(),
				entity.getNewNotes(),
				entity.getChangedAt(),
				entity.getCreatedAt());
	}

}
