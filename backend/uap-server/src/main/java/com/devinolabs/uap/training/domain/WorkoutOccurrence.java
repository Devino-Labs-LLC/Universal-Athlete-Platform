package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class WorkoutOccurrence {

	private static final int MAX_ATHLETE_NOTES_LENGTH = 4000;

	private final WorkoutOccurrenceId id;
	private final TrainingPlanId trainingPlanId;
	private final WorkoutDayId workoutDayId;
	private final AthleteId athleteId;
	private LocalDate scheduledDate;
	private LocalTime plannedStartTime;
	private Instant startedAt;
	private Instant completedAt;
	private WorkoutOccurrenceStatus status;
	private String athleteNotes;
	private final WorkoutOccurrenceOrigin origin;
	private final WorkoutGenerationKey generationKey;
	private LocalDate originalScheduledDate;
	private boolean manuallyRescheduled;
	private WorkoutOccurrenceEnvironmentSnapshot plannedEnvironment;
	private WorkoutOccurrenceEnvironmentSnapshot actualEnvironment;
	private Instant environmentSelectedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutOccurrence(
			WorkoutOccurrenceId id,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			Instant startedAt,
			Instant completedAt,
			WorkoutOccurrenceStatus status,
			String athleteNotes,
			WorkoutOccurrenceOrigin origin,
			WorkoutGenerationKey generationKey,
			LocalDate originalScheduledDate,
			boolean manuallyRescheduled,
			WorkoutOccurrenceEnvironmentSnapshot plannedEnvironment,
			WorkoutOccurrenceEnvironmentSnapshot actualEnvironment,
			Instant environmentSelectedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.scheduledDate = requireScheduledDate(scheduledDate);
		this.plannedStartTime = plannedStartTime;
		this.startedAt = startedAt;
		this.completedAt = normalizeCompletedAt(completedAt, status);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.athleteNotes = normalizeAthleteNotes(athleteNotes);
		this.origin = Objects.requireNonNull(origin, "origin must not be null");
		this.generationKey = normalizeGenerationKey(origin, generationKey);
		this.originalScheduledDate = originalScheduledDate;
		this.manuallyRescheduled = manuallyRescheduled;
		this.plannedEnvironment = plannedEnvironment;
		this.actualEnvironment = actualEnvironment;
		this.environmentSelectedAt = environmentSelectedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutOccurrence createManual(
			WorkoutOccurrenceId id,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			String athleteNotes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutOccurrence(
				id,
				trainingPlanId,
				workoutDayId,
				athleteId,
				scheduledDate,
				plannedStartTime,
				null,
				null,
				WorkoutOccurrenceStatus.SCHEDULED,
				athleteNotes,
				WorkoutOccurrenceOrigin.MANUAL,
				null,
				null,
				false,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static WorkoutOccurrence createGenerated(
			WorkoutOccurrenceId id,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			WorkoutGenerationKey generationKey,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(generationKey, "generationKey must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutOccurrence(
				id,
				trainingPlanId,
				workoutDayId,
				athleteId,
				scheduledDate,
				plannedStartTime,
				null,
				null,
				WorkoutOccurrenceStatus.SCHEDULED,
				null,
				WorkoutOccurrenceOrigin.GENERATED,
				generationKey,
				null,
				false,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static WorkoutOccurrence rehydrate(
			WorkoutOccurrenceId id,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			Instant startedAt,
			Instant completedAt,
			WorkoutOccurrenceStatus status,
			String athleteNotes,
			WorkoutOccurrenceOrigin origin,
			WorkoutGenerationKey generationKey,
			LocalDate originalScheduledDate,
			boolean manuallyRescheduled,
			WorkoutOccurrenceEnvironmentSnapshot plannedEnvironment,
			WorkoutOccurrenceEnvironmentSnapshot actualEnvironment,
			Instant environmentSelectedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutOccurrence(
				id,
				trainingPlanId,
				workoutDayId,
				athleteId,
				scheduledDate,
				plannedStartTime,
				startedAt,
				completedAt,
				status,
				athleteNotes,
				origin,
				generationKey,
				originalScheduledDate,
				manuallyRescheduled,
				plannedEnvironment,
				actualEnvironment,
				environmentSelectedAt,
				createdAt,
				updatedAt,
				version);
	}

	public void initializeEnvironmentContext(WorkoutOccurrenceEnvironmentSnapshot planned, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.plannedEnvironment = planned;
		if (planned != null) {
			this.actualEnvironment = WorkoutOccurrenceEnvironmentSnapshot.of(
					planned.trainingEnvironmentId(),
					planned.nameSnapshot(),
					planned.availableEquipmentSnapshot());
			this.environmentSelectedAt = Instant.now(clock);
		}
		else {
			this.actualEnvironment = null;
			this.environmentSelectedAt = null;
		}
		touch(clock);
	}

	public void setActualEnvironment(WorkoutOccurrenceEnvironmentSnapshot snapshot, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		this.actualEnvironment = snapshot;
		this.environmentSelectedAt = Instant.now(clock);
		touch(clock);
	}

	public void clearActualEnvironment(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (actualEnvironment == null) {
			throw new IllegalStateException("Actual environment is not set");
		}
		this.actualEnvironment = null;
		this.environmentSelectedAt = null;
		touch(clock);
	}

	public void start(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutOccurrenceStatus.IN_PROGRESS) {
			return;
		}
		if (status != WorkoutOccurrenceStatus.SCHEDULED) {
			throw new IllegalStateException("Only SCHEDULED workout occurrences can be started");
		}
		if (startedAt == null) {
			this.startedAt = Instant.now(clock);
		}
		this.status = WorkoutOccurrenceStatus.IN_PROGRESS;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutOccurrenceStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutOccurrenceStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only IN_PROGRESS workout occurrences can be completed");
		}
		this.status = WorkoutOccurrenceStatus.COMPLETED;
		this.completedAt = Instant.now(clock);
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutOccurrenceStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutOccurrenceStatus.SCHEDULED && status != WorkoutOccurrenceStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only SCHEDULED or IN_PROGRESS workout occurrences can be skipped");
		}
		this.status = WorkoutOccurrenceStatus.SKIPPED;
		this.completedAt = null;
		touch(clock);
	}

	public void cancel(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutOccurrenceStatus.CANCELLED) {
			return;
		}
		if (status != WorkoutOccurrenceStatus.SCHEDULED) {
			throw new IllegalStateException("Only SCHEDULED workout occurrences can be cancelled");
		}
		this.status = WorkoutOccurrenceStatus.CANCELLED;
		this.completedAt = null;
		touch(clock);
	}

	public void updateDetails(LocalTime plannedStartTime, String athleteNotes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireMutable();
		this.plannedStartTime = plannedStartTime;
		this.athleteNotes = normalizeAthleteNotes(athleteNotes);
		touch(clock);
	}

	public void changeScheduledDate(LocalDate scheduledDate, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireMutable();
		this.scheduledDate = requireScheduledDate(scheduledDate);
		touch(clock);
	}

	/**
	 * Moves an untouched occurrence to a new slot. The generation key is preserved so the generator
	 * recognises the placement as already materialised and does not recreate it at the original date.
	 */
	public void reschedule(LocalDate newScheduledDate, LocalTime newPlannedStartTime, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		LocalDate target = requireScheduledDate(newScheduledDate);
		if (status != WorkoutOccurrenceStatus.SCHEDULED) {
			throw new IllegalStateException("Only SCHEDULED workout occurrences can be rescheduled");
		}
		if (originalScheduledDate == null) {
			this.originalScheduledDate = this.scheduledDate;
		}
		this.scheduledDate = target;
		this.plannedStartTime = newPlannedStartTime;
		this.manuallyRescheduled = true;
		touch(clock);
	}

	private void requireMutable() {
		if (status != WorkoutOccurrenceStatus.SCHEDULED && status != WorkoutOccurrenceStatus.IN_PROGRESS) {
			throw new IllegalStateException(
					"Only SCHEDULED or IN_PROGRESS workout occurrences can be updated");
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static LocalDate requireScheduledDate(LocalDate scheduledDate) {
		Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		return scheduledDate;
	}

	private static WorkoutGenerationKey normalizeGenerationKey(
			WorkoutOccurrenceOrigin origin,
			WorkoutGenerationKey generationKey) {
		if (origin == WorkoutOccurrenceOrigin.MANUAL && generationKey != null) {
			throw new IllegalArgumentException("generationKey is only allowed for GENERATED occurrences");
		}
		return generationKey;
	}

	private static Instant normalizeCompletedAt(Instant completedAt, WorkoutOccurrenceStatus status) {
		if (status == WorkoutOccurrenceStatus.COMPLETED) {
			if (completedAt == null) {
				throw new IllegalArgumentException("completedAt is required when status is COMPLETED");
			}
			return completedAt;
		}
		if (completedAt != null) {
			throw new IllegalArgumentException("completedAt is only allowed when status is COMPLETED");
		}
		return null;
	}

	private static String normalizeAthleteNotes(String athleteNotes) {
		if (athleteNotes == null || athleteNotes.isBlank()) {
			return null;
		}
		String trimmed = athleteNotes.trim();
		if (trimmed.length() > MAX_ATHLETE_NOTES_LENGTH) {
			throw new IllegalArgumentException(
					"athleteNotes must not exceed " + MAX_ATHLETE_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	public WorkoutOccurrenceId id() {
		return id;
	}

	public TrainingPlanId trainingPlanId() {
		return trainingPlanId;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public LocalDate scheduledDate() {
		return scheduledDate;
	}

	public LocalTime plannedStartTime() {
		return plannedStartTime;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant completedAt() {
		return completedAt;
	}

	public WorkoutOccurrenceStatus status() {
		return status;
	}

	public String athleteNotes() {
		return athleteNotes;
	}

	public WorkoutOccurrenceOrigin origin() {
		return origin;
	}

	public WorkoutGenerationKey generationKey() {
		return generationKey;
	}

	public LocalDate originalScheduledDate() {
		return originalScheduledDate;
	}

	public boolean manuallyRescheduled() {
		return manuallyRescheduled;
	}

	public WorkoutOccurrenceEnvironmentSnapshot plannedEnvironment() {
		return plannedEnvironment;
	}

	public WorkoutOccurrenceEnvironmentSnapshot actualEnvironment() {
		return actualEnvironment;
	}

	public Instant environmentSelectedAt() {
		return environmentSelectedAt;
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
