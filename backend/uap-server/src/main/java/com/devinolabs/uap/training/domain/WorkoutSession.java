package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class WorkoutSession {

	private static final int MAX_ATHLETE_NOTES_LENGTH = 4000;

	private final WorkoutSessionId id;
	private final WorkoutExerciseId workoutExerciseId;
	private final WorkoutDayId workoutDayId;
	private final AthleteId athleteId;
	private WorkoutSessionStatus status;
	private Integer actualSets;
	private Integer actualReps;
	private BigDecimal actualWeight;
	private WeightUnit weightUnit;
	private Integer actualDurationSeconds;
	private BigDecimal actualDistance;
	private DistanceUnit distanceUnit;
	private Integer actualRestSeconds;
	private Integer actualRpe;
	private Instant completedAt;
	private String athleteNotes;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutSession(
			WorkoutSessionId id,
			WorkoutExerciseId workoutExerciseId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			WorkoutSessionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.workoutExerciseId = Objects.requireNonNull(workoutExerciseId, "workoutExerciseId must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		Execution execution = normalizeExecution(
				actualSets,
				actualReps,
				actualWeight,
				weightUnit,
				actualDurationSeconds,
				actualDistance,
				distanceUnit,
				actualRestSeconds,
				actualRpe,
				completedAt,
				athleteNotes,
				status);
		this.actualSets = execution.actualSets();
		this.actualReps = execution.actualReps();
		this.actualWeight = execution.actualWeight();
		this.weightUnit = execution.weightUnit();
		this.actualDurationSeconds = execution.actualDurationSeconds();
		this.actualDistance = execution.actualDistance();
		this.distanceUnit = execution.distanceUnit();
		this.actualRestSeconds = execution.actualRestSeconds();
		this.actualRpe = execution.actualRpe();
		this.completedAt = execution.completedAt();
		this.athleteNotes = execution.athleteNotes();
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutSession create(
			WorkoutSessionId id,
			WorkoutExerciseId workoutExerciseId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutSession(
				id,
				workoutExerciseId,
				workoutDayId,
				athleteId,
				WorkoutSessionStatus.NOT_STARTED,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static WorkoutSession rehydrate(
			WorkoutSessionId id,
			WorkoutExerciseId workoutExerciseId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			WorkoutSessionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutSession(
				id,
				workoutExerciseId,
				workoutDayId,
				athleteId,
				status,
				actualSets,
				actualReps,
				actualWeight,
				weightUnit,
				actualDurationSeconds,
				actualDistance,
				distanceUnit,
				actualRestSeconds,
				actualRpe,
				completedAt,
				athleteNotes,
				createdAt,
				updatedAt,
				version);
	}

	public void start(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutSessionStatus.IN_PROGRESS) {
			return;
		}
		if (status != WorkoutSessionStatus.NOT_STARTED) {
			throw new IllegalStateException("Only NOT_STARTED workout sessions can be started");
		}
		this.status = WorkoutSessionStatus.IN_PROGRESS;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutSessionStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutSessionStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only IN_PROGRESS workout sessions can be completed");
		}
		this.status = WorkoutSessionStatus.COMPLETED;
		this.completedAt = Instant.now(clock);
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutSessionStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutSessionStatus.NOT_STARTED && status != WorkoutSessionStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only NOT_STARTED or IN_PROGRESS workout sessions can be skipped");
		}
		this.status = WorkoutSessionStatus.SKIPPED;
		this.completedAt = null;
		touch(clock);
	}

	public void updateExecution(
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Execution execution = normalizeExecution(
				actualSets,
				actualReps,
				actualWeight,
				weightUnit,
				actualDurationSeconds,
				actualDistance,
				distanceUnit,
				actualRestSeconds,
				actualRpe,
				this.completedAt,
				this.athleteNotes,
				this.status);
		this.actualSets = execution.actualSets();
		this.actualReps = execution.actualReps();
		this.actualWeight = execution.actualWeight();
		this.weightUnit = execution.weightUnit();
		this.actualDurationSeconds = execution.actualDurationSeconds();
		this.actualDistance = execution.actualDistance();
		this.distanceUnit = execution.distanceUnit();
		this.actualRestSeconds = execution.actualRestSeconds();
		this.actualRpe = execution.actualRpe();
		touch(clock);
	}

	public void updateNotes(String athleteNotes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.athleteNotes = normalizeAthleteNotes(athleteNotes);
		touch(clock);
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static Execution normalizeExecution(
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Instant completedAt,
			String athleteNotes,
			WorkoutSessionStatus status) {
		if (actualSets != null && actualSets < 0) {
			throw new IllegalArgumentException("actualSets must be >= 0");
		}
		if (actualReps != null && actualReps < 0) {
			throw new IllegalArgumentException("actualReps must be >= 0");
		}
		if ((actualWeight == null) != (weightUnit == null)) {
			throw new IllegalArgumentException("actualWeight and weightUnit must both be provided or both omitted");
		}
		if (actualWeight != null && actualWeight.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("actualWeight must be >= 0");
		}
		if (actualDurationSeconds != null && actualDurationSeconds < 0) {
			throw new IllegalArgumentException("actualDurationSeconds must be >= 0");
		}
		if ((actualDistance == null) != (distanceUnit == null)) {
			throw new IllegalArgumentException("actualDistance and distanceUnit must both be provided or both omitted");
		}
		if (actualDistance != null && actualDistance.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("actualDistance must be >= 0");
		}
		if (actualRestSeconds != null && actualRestSeconds < 0) {
			throw new IllegalArgumentException("actualRestSeconds must be >= 0");
		}
		if (actualRpe != null && (actualRpe < 0 || actualRpe > 10)) {
			throw new IllegalArgumentException("actualRpe must be between 0 and 10");
		}
		if (status == WorkoutSessionStatus.COMPLETED && completedAt == null) {
			throw new IllegalArgumentException("completedAt is required when status is COMPLETED");
		}
		if (status != WorkoutSessionStatus.COMPLETED && completedAt != null) {
			throw new IllegalArgumentException("completedAt is only allowed when status is COMPLETED");
		}
		return new Execution(
				actualSets,
				actualReps,
				actualWeight,
				weightUnit,
				actualDurationSeconds,
				actualDistance,
				distanceUnit,
				actualRestSeconds,
				actualRpe,
				completedAt,
				normalizeAthleteNotes(athleteNotes));
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

	private record Execution(
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Instant completedAt,
			String athleteNotes) {
	}

	public WorkoutSessionId id() {
		return id;
	}

	public WorkoutExerciseId workoutExerciseId() {
		return workoutExerciseId;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public WorkoutSessionStatus status() {
		return status;
	}

	public Integer actualSets() {
		return actualSets;
	}

	public Integer actualReps() {
		return actualReps;
	}

	public BigDecimal actualWeight() {
		return actualWeight;
	}

	public WeightUnit weightUnit() {
		return weightUnit;
	}

	public Integer actualDurationSeconds() {
		return actualDurationSeconds;
	}

	public BigDecimal actualDistance() {
		return actualDistance;
	}

	public DistanceUnit distanceUnit() {
		return distanceUnit;
	}

	public Integer actualRestSeconds() {
		return actualRestSeconds;
	}

	public Integer actualRpe() {
		return actualRpe;
	}

	public Instant completedAt() {
		return completedAt;
	}

	public String athleteNotes() {
		return athleteNotes;
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
