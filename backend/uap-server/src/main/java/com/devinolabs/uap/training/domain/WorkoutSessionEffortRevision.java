package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.util.Objects;

public final class WorkoutSessionEffortRevision {

	private final WorkoutSessionEffortRevisionId id;
	private final WorkoutSessionEffortId workoutSessionEffortId;
	private final AthleteId athleteId;
	private final int revisionNumber;
	private final SessionRpe priorSessionRpe;
	private final SessionRpe newSessionRpe;
	private final Integer priorDurationMinutes;
	private final Integer newDurationMinutes;
	private final String priorNotes;
	private final String newNotes;
	private final Instant changedAt;
	private final Instant createdAt;

	private WorkoutSessionEffortRevision(
			WorkoutSessionEffortRevisionId id,
			WorkoutSessionEffortId workoutSessionEffortId,
			AthleteId athleteId,
			int revisionNumber,
			SessionRpe priorSessionRpe,
			SessionRpe newSessionRpe,
			Integer priorDurationMinutes,
			Integer newDurationMinutes,
			String priorNotes,
			String newNotes,
			Instant changedAt,
			Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.workoutSessionEffortId = Objects.requireNonNull(
				workoutSessionEffortId, "workoutSessionEffortId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		if (revisionNumber < 1) {
			throw new IllegalArgumentException("revisionNumber must be >= 1");
		}
		this.revisionNumber = revisionNumber;
		this.priorSessionRpe = Objects.requireNonNull(priorSessionRpe, "priorSessionRpe must not be null");
		this.newSessionRpe = Objects.requireNonNull(newSessionRpe, "newSessionRpe must not be null");
		this.priorDurationMinutes = priorDurationMinutes;
		this.newDurationMinutes = newDurationMinutes;
		this.priorNotes = priorNotes;
		this.newNotes = newNotes;
		this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	static WorkoutSessionEffortRevision create(
			WorkoutSessionEffort effort,
			SessionRpe newSessionRpe,
			Integer explicitDurationMinutes,
			String newNotes,
			WorkoutOccurrence occurrence,
			int revisionNumber,
			Instant changedAt) {
		SessionDurationResolver.ResolvedSessionDuration resolved =
				SessionDurationResolver.resolve(explicitDurationMinutes, occurrence);
		return new WorkoutSessionEffortRevision(
				WorkoutSessionEffortRevisionId.generate(),
				effort.id(),
				effort.athleteId(),
				revisionNumber,
				effort.sessionRpe(),
				newSessionRpe,
				effort.sessionDurationMinutes(),
				resolved.minutes(),
				effort.perceivedNotes(),
				WorkoutSessionEffort.normalizeNotes(newNotes),
				changedAt,
				changedAt);
	}

	public static WorkoutSessionEffortRevision rehydrate(
			WorkoutSessionEffortRevisionId id,
			WorkoutSessionEffortId workoutSessionEffortId,
			AthleteId athleteId,
			int revisionNumber,
			SessionRpe priorSessionRpe,
			SessionRpe newSessionRpe,
			Integer priorDurationMinutes,
			Integer newDurationMinutes,
			String priorNotes,
			String newNotes,
			Instant changedAt,
			Instant createdAt) {
		return new WorkoutSessionEffortRevision(
				id,
				workoutSessionEffortId,
				athleteId,
				revisionNumber,
				priorSessionRpe,
				newSessionRpe,
				priorDurationMinutes,
				newDurationMinutes,
				priorNotes,
				newNotes,
				changedAt,
				createdAt);
	}

	public WorkoutSessionEffortRevisionId id() {
		return id;
	}

	public WorkoutSessionEffortId workoutSessionEffortId() {
		return workoutSessionEffortId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int revisionNumber() {
		return revisionNumber;
	}

	public SessionRpe priorSessionRpe() {
		return priorSessionRpe;
	}

	public SessionRpe newSessionRpe() {
		return newSessionRpe;
	}

	public Integer priorDurationMinutes() {
		return priorDurationMinutes;
	}

	public Integer newDurationMinutes() {
		return newDurationMinutes;
	}

	public String priorNotes() {
		return priorNotes;
	}

	public String newNotes() {
		return newNotes;
	}

	public Instant changedAt() {
		return changedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
