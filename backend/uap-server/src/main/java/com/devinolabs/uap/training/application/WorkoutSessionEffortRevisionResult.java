package com.devinolabs.uap.training.application;

import java.time.Instant;

import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevision;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevisionId;

public record WorkoutSessionEffortRevisionResult(
		WorkoutSessionEffortRevisionId id,
		int revisionNumber,
		SessionRpe priorSessionRpe,
		SessionRpe newSessionRpe,
		Integer priorDurationMinutes,
		Integer newDurationMinutes,
		String priorNotes,
		String newNotes,
		Instant changedAt) {

	public static WorkoutSessionEffortRevisionResult from(WorkoutSessionEffortRevision revision) {
		return new WorkoutSessionEffortRevisionResult(
				revision.id(),
				revision.revisionNumber(),
				revision.priorSessionRpe(),
				revision.newSessionRpe(),
				revision.priorDurationMinutes(),
				revision.newDurationMinutes(),
				revision.priorNotes(),
				revision.newNotes(),
				revision.changedAt());
	}
}
