package com.devinolabs.uap.training.application;

import java.time.Instant;

import com.devinolabs.uap.training.domain.SessionDurationSource;
import com.devinolabs.uap.training.domain.SessionEffortSource;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;
import com.devinolabs.uap.training.domain.TrainingPlanId;

public record WorkoutSessionEffortResult(
		WorkoutSessionEffortId id,
		TrainingPlanId trainingPlanId,
		WorkoutDayId workoutDayId,
		WorkoutOccurrenceId workoutOccurrenceId,
		SessionRpe sessionRpe,
		Integer sessionDurationMinutes,
		SessionDurationSource durationSource,
		String perceivedNotes,
		Instant submittedAt,
		SessionEffortSource effortSource,
		Instant createdAt,
		Instant updatedAt) {

	public static WorkoutSessionEffortResult from(WorkoutSessionEffort effort) {
		return new WorkoutSessionEffortResult(
				effort.id(),
				effort.trainingPlanId(),
				effort.workoutDayId(),
				effort.workoutOccurrenceId(),
				effort.sessionRpe(),
				effort.sessionDurationMinutes(),
				effort.durationSource(),
				effort.perceivedNotes(),
				effort.submittedAt(),
				effort.effortSource(),
				effort.createdAt(),
				effort.updatedAt());
	}
}
