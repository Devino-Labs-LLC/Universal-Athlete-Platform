package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Athlete-reported session effort for one completed occurrence.
 */
public final class WorkoutSessionEffort {

	private static final int MAX_NOTES_LENGTH = 1000;

	private final WorkoutSessionEffortId id;
	private final AthleteId athleteId;
	private final TrainingPlanId trainingPlanId;
	private final WorkoutDayId workoutDayId;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private SessionRpe sessionRpe;
	private Integer sessionDurationMinutes;
	private SessionDurationSource durationSource;
	private String perceivedNotes;
	private Instant submittedAt;
	private final SessionEffortSource effortSource;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutSessionEffort(
			WorkoutSessionEffortId id,
			AthleteId athleteId,
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
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.sessionRpe = Objects.requireNonNull(sessionRpe, "sessionRpe must not be null");
		this.sessionDurationMinutes = sessionDurationMinutes;
		this.durationSource = Objects.requireNonNull(durationSource, "durationSource must not be null");
		this.perceivedNotes = normalizeNotes(perceivedNotes);
		this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
		this.effortSource = Objects.requireNonNull(effortSource, "effortSource must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutSessionEffort create(
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			SessionRpe sessionRpe,
			Integer explicitDurationMinutes,
			String perceivedNotes,
			WorkoutOccurrence occurrence,
			SessionEffortSource effortSource,
			Clock clock) {
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		SessionDurationResolver.ResolvedSessionDuration resolved =
				SessionDurationResolver.resolve(explicitDurationMinutes, occurrence);
		return new WorkoutSessionEffort(
				WorkoutSessionEffortId.generate(),
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				sessionRpe,
				resolved.minutes(),
				resolved.source(),
				perceivedNotes,
				now,
				effortSource,
				now,
				now,
				0L);
	}

	public static WorkoutSessionEffort rehydrate(
			WorkoutSessionEffortId id,
			AthleteId athleteId,
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
			Instant updatedAt,
			long version) {
		return new WorkoutSessionEffort(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				sessionRpe,
				sessionDurationMinutes,
				durationSource,
				perceivedNotes,
				submittedAt,
				effortSource,
				createdAt,
				updatedAt,
				version);
	}

	public WorkoutSessionEffortRevision update(
			SessionRpe newSessionRpe,
			Integer explicitDurationMinutes,
			String newNotes,
			WorkoutOccurrence occurrence,
			int nextRevisionNumber,
			Clock clock) {
		Objects.requireNonNull(newSessionRpe, "newSessionRpe must not be null");
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant changedAt = Instant.now(clock);
		SessionDurationResolver.ResolvedSessionDuration resolved =
				SessionDurationResolver.resolve(explicitDurationMinutes, occurrence);
		WorkoutSessionEffortRevision revision = WorkoutSessionEffortRevision.create(
				this,
				newSessionRpe,
				explicitDurationMinutes,
				newNotes,
				occurrence,
				nextRevisionNumber,
				changedAt);
		this.sessionRpe = newSessionRpe;
		this.sessionDurationMinutes = resolved.minutes();
		this.durationSource = resolved.source();
		this.perceivedNotes = revision.newNotes();
		this.submittedAt = changedAt;
		this.updatedAt = changedAt;
		return revision;
	}

	static String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String trimmed = notes.trim();
		if (trimmed.length() > MAX_NOTES_LENGTH) {
			throw new InvalidWorkoutSessionEffortNotesException(
					"perceivedNotes must not exceed " + MAX_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	public WorkoutSessionEffortId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public TrainingPlanId trainingPlanId() {
		return trainingPlanId;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public SessionRpe sessionRpe() {
		return sessionRpe;
	}

	public Integer sessionDurationMinutes() {
		return sessionDurationMinutes;
	}

	public SessionDurationSource durationSource() {
		return durationSource;
	}

	public String perceivedNotes() {
		return perceivedNotes;
	}

	public Instant submittedAt() {
		return submittedAt;
	}

	public SessionEffortSource effortSource() {
		return effortSource;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
