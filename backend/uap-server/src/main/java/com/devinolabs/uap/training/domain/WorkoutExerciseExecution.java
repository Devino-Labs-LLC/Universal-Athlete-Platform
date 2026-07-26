package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class WorkoutExerciseExecution {

	private static final int MAX_ATHLETE_NOTES_LENGTH = 4000;

	private final WorkoutExerciseExecutionId id;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final WorkoutExerciseId sourceWorkoutExerciseId;
	private final AthleteId athleteId;
	private final int displayOrder;
	private final String exerciseName;
	private final ExerciseCategory category;
	private final ExerciseType type;
	private final Integer prescribedSets;
	private final Integer prescribedMinimumReps;
	private final Integer prescribedMaximumReps;
	private final BigDecimal prescribedTargetWeight;
	private final WeightUnit prescribedWeightUnit;
	private final Integer prescribedTargetDurationSeconds;
	private final BigDecimal prescribedTargetDistance;
	private final DistanceUnit prescribedDistanceUnit;
	private final Integer prescribedTargetRestSeconds;
	private final Integer prescribedTargetRpe;
	private final String prescribedTempo;
	private final String prescribedCoachingNotes;
	private WorkoutExerciseExecutionStatus status;
	private Integer actualSets;
	private Integer actualReps;
	private BigDecimal actualWeight;
	private WeightUnit weightUnit;
	private Integer actualDurationSeconds;
	private BigDecimal actualDistance;
	private DistanceUnit distanceUnit;
	private Integer actualRestSeconds;
	private BigDecimal actualRpe;
	private Instant startedAt;
	private Instant completedAt;
	private String athleteNotes;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutExerciseExecution(
			WorkoutExerciseExecutionId id,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutExerciseId sourceWorkoutExerciseId,
			AthleteId athleteId,
			int displayOrder,
			String exerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer prescribedSets,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedTargetWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedTargetDurationSeconds,
			BigDecimal prescribedTargetDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRestSeconds,
			Integer prescribedTargetRpe,
			String prescribedTempo,
			String prescribedCoachingNotes,
			WorkoutExerciseExecutionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.sourceWorkoutExerciseId = Objects.requireNonNull(
				sourceWorkoutExerciseId, "sourceWorkoutExerciseId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		this.exerciseName = Objects.requireNonNull(exerciseName, "exerciseName must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.prescribedSets = Objects.requireNonNull(prescribedSets, "prescribedSets must not be null");
		this.prescribedMinimumReps = prescribedMinimumReps;
		this.prescribedMaximumReps = prescribedMaximumReps;
		this.prescribedTargetWeight = prescribedTargetWeight;
		this.prescribedWeightUnit = prescribedWeightUnit;
		this.prescribedTargetDurationSeconds = prescribedTargetDurationSeconds;
		this.prescribedTargetDistance = prescribedTargetDistance;
		this.prescribedDistanceUnit = prescribedDistanceUnit;
		this.prescribedTargetRestSeconds = prescribedTargetRestSeconds;
		this.prescribedTargetRpe = prescribedTargetRpe;
		this.prescribedTempo = prescribedTempo;
		this.prescribedCoachingNotes = prescribedCoachingNotes;
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
				startedAt,
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
		this.startedAt = execution.startedAt();
		this.completedAt = execution.completedAt();
		this.athleteNotes = execution.athleteNotes();
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutExerciseExecution fromPrescription(
			WorkoutExercise exercise,
			WorkoutOccurrenceId occurrenceId,
			Clock clock) {
		Objects.requireNonNull(exercise, "exercise must not be null");
		Objects.requireNonNull(occurrenceId, "occurrenceId must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutExerciseExecution(
				WorkoutExerciseExecutionId.generate(),
				occurrenceId,
				exercise.id(),
				exercise.athleteId(),
				exercise.displayOrder(),
				exercise.exerciseName(),
				exercise.category(),
				exercise.type(),
				exercise.sets(),
				exercise.minimumReps(),
				exercise.maximumReps(),
				exercise.targetWeight(),
				exercise.weightUnit(),
				exercise.targetDurationSeconds(),
				exercise.targetDistance(),
				exercise.distanceUnit(),
				exercise.targetRestSeconds(),
				exercise.targetRpe(),
				exercise.tempo(),
				exercise.coachingNotes(),
				WorkoutExerciseExecutionStatus.NOT_STARTED,
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
				null,
				now,
				now,
				0L);
	}

	public static WorkoutExerciseExecution rehydrate(
			WorkoutExerciseExecutionId id,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutExerciseId sourceWorkoutExerciseId,
			AthleteId athleteId,
			int displayOrder,
			String exerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer prescribedSets,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedTargetWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedTargetDurationSeconds,
			BigDecimal prescribedTargetDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRestSeconds,
			Integer prescribedTargetRpe,
			String prescribedTempo,
			String prescribedCoachingNotes,
			WorkoutExerciseExecutionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutExerciseExecution(
				id,
				workoutOccurrenceId,
				sourceWorkoutExerciseId,
				athleteId,
				displayOrder,
				exerciseName,
				category,
				type,
				prescribedSets,
				prescribedMinimumReps,
				prescribedMaximumReps,
				prescribedTargetWeight,
				prescribedWeightUnit,
				prescribedTargetDurationSeconds,
				prescribedTargetDistance,
				prescribedDistanceUnit,
				prescribedTargetRestSeconds,
				prescribedTargetRpe,
				prescribedTempo,
				prescribedCoachingNotes,
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
				startedAt,
				completedAt,
				athleteNotes,
				createdAt,
				updatedAt,
				version);
	}

	public void start(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseExecutionStatus.IN_PROGRESS) {
			return;
		}
		if (status != WorkoutExerciseExecutionStatus.NOT_STARTED) {
			throw new IllegalStateException("Only NOT_STARTED workout exercise executions can be started");
		}
		this.status = WorkoutExerciseExecutionStatus.IN_PROGRESS;
		this.startedAt = Instant.now(clock);
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseExecutionStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutExerciseExecutionStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only IN_PROGRESS workout exercise executions can be completed");
		}
		this.status = WorkoutExerciseExecutionStatus.COMPLETED;
		this.completedAt = Instant.now(clock);
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseExecutionStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutExerciseExecutionStatus.NOT_STARTED
				&& status != WorkoutExerciseExecutionStatus.IN_PROGRESS) {
			throw new IllegalStateException(
					"Only NOT_STARTED or IN_PROGRESS workout exercise executions can be skipped");
		}
		this.status = WorkoutExerciseExecutionStatus.SKIPPED;
		this.completedAt = null;
		touch(clock);
	}

	/**
	 * Replaces the summary aggregates with values derived from the execution's completed sets.
	 * Actuals are never written directly by clients; see Phase 7G set-level logging.
	 */
	public void applyDerivedActuals(
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
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
				this.startedAt,
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

	private static int requireDisplayOrder(int displayOrder) {
		if (displayOrder < 0) {
			throw new IllegalArgumentException("displayOrder must not be negative");
		}
		return displayOrder;
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
			BigDecimal actualRpe,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			WorkoutExerciseExecutionStatus status) {
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
		if (actualRpe != null
				&& (actualRpe.compareTo(BigDecimal.ZERO) < 0 || actualRpe.compareTo(BigDecimal.TEN) > 0)) {
			throw new IllegalArgumentException("actualRpe must be between 0 and 10");
		}
		if (status == WorkoutExerciseExecutionStatus.COMPLETED && completedAt == null) {
			throw new IllegalArgumentException("completedAt is required when status is COMPLETED");
		}
		if (status != WorkoutExerciseExecutionStatus.COMPLETED && completedAt != null) {
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
				startedAt,
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
			BigDecimal actualRpe,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes) {
	}

	public WorkoutExerciseExecutionId id() {
		return id;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public WorkoutExerciseId sourceWorkoutExerciseId() {
		return sourceWorkoutExerciseId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int displayOrder() {
		return displayOrder;
	}

	public String exerciseName() {
		return exerciseName;
	}

	public ExerciseCategory category() {
		return category;
	}

	public ExerciseType type() {
		return type;
	}

	public Integer prescribedSets() {
		return prescribedSets;
	}

	public Integer prescribedMinimumReps() {
		return prescribedMinimumReps;
	}

	public Integer prescribedMaximumReps() {
		return prescribedMaximumReps;
	}

	public BigDecimal prescribedTargetWeight() {
		return prescribedTargetWeight;
	}

	public WeightUnit prescribedWeightUnit() {
		return prescribedWeightUnit;
	}

	public Integer prescribedTargetDurationSeconds() {
		return prescribedTargetDurationSeconds;
	}

	public BigDecimal prescribedTargetDistance() {
		return prescribedTargetDistance;
	}

	public DistanceUnit prescribedDistanceUnit() {
		return prescribedDistanceUnit;
	}

	public Integer prescribedTargetRestSeconds() {
		return prescribedTargetRestSeconds;
	}

	public Integer prescribedTargetRpe() {
		return prescribedTargetRpe;
	}

	public String prescribedTempo() {
		return prescribedTempo;
	}

	public String prescribedCoachingNotes() {
		return prescribedCoachingNotes;
	}

	public WorkoutExerciseExecutionStatus status() {
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

	public BigDecimal actualRpe() {
		return actualRpe;
	}

	public Instant startedAt() {
		return startedAt;
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
