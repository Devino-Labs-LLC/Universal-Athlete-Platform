package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutSessionEffortResult;
import com.devinolabs.uap.training.domain.SessionDurationSource;
import com.devinolabs.uap.training.domain.SessionEffortSource;

record WorkoutSessionEffortResponse(
		UUID id,
		UUID trainingPlanId,
		UUID workoutDayId,
		UUID workoutOccurrenceId,
		BigDecimal sessionRpe,
		Integer sessionDurationMinutes,
		SessionDurationSource durationSource,
		String perceivedNotes,
		Instant submittedAt,
		SessionEffortSource effortSource,
		Instant createdAt,
		Instant updatedAt) {

	static WorkoutSessionEffortResponse from(WorkoutSessionEffortResult result) {
		return new WorkoutSessionEffortResponse(
				result.id().value(),
				result.trainingPlanId().value(),
				result.workoutDayId().value(),
				result.workoutOccurrenceId().value(),
				result.sessionRpe().value(),
				result.sessionDurationMinutes(),
				result.durationSource(),
				result.perceivedNotes(),
				result.submittedAt(),
				result.effortSource(),
				result.createdAt(),
				result.updatedAt());
	}

}
