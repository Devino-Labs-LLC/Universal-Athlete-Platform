package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutSessionEffortRevisionResult;

record WorkoutSessionEffortRevisionResponse(
		UUID id,
		int revisionNumber,
		BigDecimal priorSessionRpe,
		BigDecimal newSessionRpe,
		Integer priorDurationMinutes,
		Integer newDurationMinutes,
		String priorNotes,
		String newNotes,
		Instant changedAt) {

	static WorkoutSessionEffortRevisionResponse from(WorkoutSessionEffortRevisionResult result) {
		return new WorkoutSessionEffortRevisionResponse(
				result.id().value(),
				result.revisionNumber(),
				result.priorSessionRpe().value(),
				result.newSessionRpe().value(),
				result.priorDurationMinutes(),
				result.newDurationMinutes(),
				result.priorNotes(),
				result.newNotes(),
				result.changedAt());
	}

}
