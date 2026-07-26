package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * A single prescribed-and-logged set inside a {@link WorkoutExerciseExecution}.
 *
 * <p>Sets are the only place actual performance is recorded; the parent execution derives its
 * summary aggregates from its completed sets when it is completed.
 */
public class WorkoutExerciseSet {

	public static final int MAX_ATHLETE_NOTES_LENGTH = 2000;

	private static final BigDecimal MAX_RPE = BigDecimal.TEN;

	private final WorkoutExerciseSetId id;
	private final WorkoutExerciseExecutionId workoutExerciseExecutionId;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final AthleteId athleteId;
	private int setNumber;
	private int displayOrder;
	private WorkoutExerciseSetType setType;
	private final Integer prescribedMinimumReps;
	private final Integer prescribedMaximumReps;
	private final BigDecimal prescribedWeight;
	private final WeightUnit prescribedWeightUnit;
	private final Integer prescribedDurationSeconds;
	private final BigDecimal prescribedDistance;
	private final DistanceUnit prescribedDistanceUnit;
	private final Integer prescribedTargetRpe;
	private final Integer prescribedRestSeconds;
	private Integer actualReps;
	private BigDecimal actualWeight;
	private WeightUnit actualWeightUnit;
	private Integer actualDurationSeconds;
	private BigDecimal actualDistance;
	private DistanceUnit actualDistanceUnit;
	private Integer actualRestSeconds;
	private BigDecimal actualRpe;
	private WorkoutExerciseSetStatus status;
	private Instant startedAt;
	private Instant completedAt;
	private String athleteNotes;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutExerciseSet(
			WorkoutExerciseSetId id,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			WorkoutOccurrenceId workoutOccurrenceId,
			AthleteId athleteId,
			int setNumber,
			int displayOrder,
			WorkoutExerciseSetType setType,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedDurationSeconds,
			BigDecimal prescribedDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRpe,
			Integer prescribedRestSeconds,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit actualWeightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit actualDistanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			WorkoutExerciseSetStatus status,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.workoutExerciseExecutionId = Objects.requireNonNull(
				workoutExerciseExecutionId, "workoutExerciseExecutionId must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.setNumber = requireSetNumber(setNumber);
		this.displayOrder = requireDisplayOrder(displayOrder);
		this.setType = Objects.requireNonNull(setType, "setType must not be null");
		requirePrescription(
				prescribedMinimumReps,
				prescribedMaximumReps,
				prescribedWeight,
				prescribedWeightUnit,
				prescribedDurationSeconds,
				prescribedDistance,
				prescribedDistanceUnit,
				prescribedTargetRpe,
				prescribedRestSeconds);
		this.prescribedMinimumReps = prescribedMinimumReps;
		this.prescribedMaximumReps = prescribedMaximumReps;
		this.prescribedWeight = prescribedWeight;
		this.prescribedWeightUnit = prescribedWeightUnit;
		this.prescribedDurationSeconds = prescribedDurationSeconds;
		this.prescribedDistance = prescribedDistance;
		this.prescribedDistanceUnit = prescribedDistanceUnit;
		this.prescribedTargetRpe = prescribedTargetRpe;
		this.prescribedRestSeconds = prescribedRestSeconds;
		requireActuals(
				actualReps,
				actualWeight,
				actualWeightUnit,
				actualDurationSeconds,
				actualDistance,
				actualDistanceUnit,
				actualRestSeconds,
				actualRpe);
		this.actualReps = actualReps;
		this.actualWeight = actualWeight;
		this.actualWeightUnit = actualWeightUnit;
		this.actualDurationSeconds = actualDurationSeconds;
		this.actualDistance = actualDistance;
		this.actualDistanceUnit = actualDistanceUnit;
		this.actualRestSeconds = actualRestSeconds;
		this.actualRpe = actualRpe;
		this.status = Objects.requireNonNull(status, "status must not be null");
		requireTimestamps(status, completedAt);
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.athleteNotes = normalizeAthleteNotes(athleteNotes);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	/**
	 * Seeds a WORKING set from the execution's frozen prescription snapshot.
	 */
	public static WorkoutExerciseSet fromExecutionPrescription(
			WorkoutExerciseExecution execution,
			int setNumber,
			int displayOrder,
			Clock clock) {
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutExerciseSet(
				WorkoutExerciseSetId.generate(),
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.athleteId(),
				setNumber,
				displayOrder,
				WorkoutExerciseSetType.WORKING,
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRpe(),
				execution.prescribedTargetRestSeconds(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				WorkoutExerciseSetStatus.NOT_STARTED,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static WorkoutExerciseSet createAdditional(
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			WorkoutOccurrenceId workoutOccurrenceId,
			AthleteId athleteId,
			int setNumber,
			int displayOrder,
			WorkoutExerciseSetType setType,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedDurationSeconds,
			BigDecimal prescribedDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRpe,
			Integer prescribedRestSeconds,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutExerciseSet(
				WorkoutExerciseSetId.generate(),
				workoutExerciseExecutionId,
				workoutOccurrenceId,
				athleteId,
				setNumber,
				displayOrder,
				setType == null ? WorkoutExerciseSetType.WORKING : setType,
				prescribedMinimumReps,
				prescribedMaximumReps,
				prescribedWeight,
				prescribedWeightUnit,
				prescribedDurationSeconds,
				prescribedDistance,
				prescribedDistanceUnit,
				prescribedTargetRpe,
				prescribedRestSeconds,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				WorkoutExerciseSetStatus.NOT_STARTED,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static WorkoutExerciseSet rehydrate(
			WorkoutExerciseSetId id,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			WorkoutOccurrenceId workoutOccurrenceId,
			AthleteId athleteId,
			int setNumber,
			int displayOrder,
			WorkoutExerciseSetType setType,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedDurationSeconds,
			BigDecimal prescribedDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRpe,
			Integer prescribedRestSeconds,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit actualWeightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit actualDistanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			WorkoutExerciseSetStatus status,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutExerciseSet(
				id,
				workoutExerciseExecutionId,
				workoutOccurrenceId,
				athleteId,
				setNumber,
				displayOrder,
				setType,
				prescribedMinimumReps,
				prescribedMaximumReps,
				prescribedWeight,
				prescribedWeightUnit,
				prescribedDurationSeconds,
				prescribedDistance,
				prescribedDistanceUnit,
				prescribedTargetRpe,
				prescribedRestSeconds,
				actualReps,
				actualWeight,
				actualWeightUnit,
				actualDurationSeconds,
				actualDistance,
				actualDistanceUnit,
				actualRestSeconds,
				actualRpe,
				status,
				startedAt,
				completedAt,
				athleteNotes,
				createdAt,
				updatedAt,
				version);
	}

	public void start(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseSetStatus.IN_PROGRESS) {
			return;
		}
		if (status != WorkoutExerciseSetStatus.NOT_STARTED) {
			throw new IllegalStateException("Only NOT_STARTED workout exercise sets can be started");
		}
		this.status = WorkoutExerciseSetStatus.IN_PROGRESS;
		this.startedAt = Instant.now(clock);
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseSetStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutExerciseSetStatus.NOT_STARTED && status != WorkoutExerciseSetStatus.IN_PROGRESS) {
			throw new IllegalStateException(
					"Only NOT_STARTED or IN_PROGRESS workout exercise sets can be completed");
		}
		Instant now = Instant.now(clock);
		if (this.startedAt == null) {
			this.startedAt = now;
		}
		this.status = WorkoutExerciseSetStatus.COMPLETED;
		this.completedAt = now;
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseSetStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutExerciseSetStatus.NOT_STARTED && status != WorkoutExerciseSetStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only NOT_STARTED or IN_PROGRESS workout exercise sets can be skipped");
		}
		this.status = WorkoutExerciseSetStatus.SKIPPED;
		this.completedAt = null;
		touch(clock);
	}

	public void updateActuals(
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit actualWeightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit actualDistanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireMutable();
		requireActuals(
				actualReps,
				actualWeight,
				actualWeightUnit,
				actualDurationSeconds,
				actualDistance,
				actualDistanceUnit,
				actualRestSeconds,
				actualRpe);
		this.actualReps = actualReps;
		this.actualWeight = actualWeight;
		this.actualWeightUnit = actualWeightUnit;
		this.actualDurationSeconds = actualDurationSeconds;
		this.actualDistance = actualDistance;
		this.actualDistanceUnit = actualDistanceUnit;
		this.actualRestSeconds = actualRestSeconds;
		this.actualRpe = actualRpe;
		touch(clock);
	}

	public void updateNotes(String athleteNotes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireMutable();
		this.athleteNotes = normalizeAthleteNotes(athleteNotes);
		touch(clock);
	}

	public void changeSetType(WorkoutExerciseSetType setType, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireMutable();
		this.setType = Objects.requireNonNull(setType, "setType must not be null");
		touch(clock);
	}

	/**
	 * Renumbering is a structural concern and stays available on terminal sets so that deleting or
	 * reordering planned sets can keep the 1..N sequence dense.
	 */
	public void changeSetNumber(int setNumber, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.setNumber = requireSetNumber(setNumber);
		touch(clock);
	}

	public void changeDisplayOrder(int displayOrder, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		touch(clock);
	}

	public void requireMutable() {
		if (status.isTerminal()) {
			throw new IllegalStateException("Workout exercise sets cannot be modified when the set is " + status);
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static int requireSetNumber(int setNumber) {
		if (setNumber < 1) {
			throw new IllegalArgumentException("setNumber must be >= 1");
		}
		return setNumber;
	}

	private static int requireDisplayOrder(int displayOrder) {
		if (displayOrder < 0) {
			throw new IllegalArgumentException("displayOrder must not be negative");
		}
		return displayOrder;
	}

	private static void requirePrescription(
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedDurationSeconds,
			BigDecimal prescribedDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRpe,
			Integer prescribedRestSeconds) {
		requireNonNegative(prescribedMinimumReps, "prescribedMinimumReps");
		requireNonNegative(prescribedMaximumReps, "prescribedMaximumReps");
		if (prescribedMinimumReps != null && prescribedMaximumReps != null
				&& prescribedMaximumReps < prescribedMinimumReps) {
			throw new IllegalArgumentException("prescribedMaximumReps must be >= prescribedMinimumReps");
		}
		requirePairedWeight(prescribedWeight, prescribedWeightUnit, "prescribedWeight", "prescribedWeightUnit");
		requireNonNegative(prescribedDurationSeconds, "prescribedDurationSeconds");
		requirePairedDistance(
				prescribedDistance, prescribedDistanceUnit, "prescribedDistance", "prescribedDistanceUnit");
		if (prescribedTargetRpe != null && (prescribedTargetRpe < 0 || prescribedTargetRpe > 10)) {
			throw new IllegalArgumentException("prescribedTargetRpe must be between 0 and 10");
		}
		requireNonNegative(prescribedRestSeconds, "prescribedRestSeconds");
	}

	private static void requireActuals(
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit actualWeightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit actualDistanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe) {
		requireNonNegative(actualReps, "actualReps");
		requirePairedWeight(actualWeight, actualWeightUnit, "actualWeight", "actualWeightUnit");
		requireNonNegative(actualDurationSeconds, "actualDurationSeconds");
		requirePairedDistance(actualDistance, actualDistanceUnit, "actualDistance", "actualDistanceUnit");
		requireNonNegative(actualRestSeconds, "actualRestSeconds");
		if (actualRpe != null
				&& (actualRpe.compareTo(BigDecimal.ZERO) < 0 || actualRpe.compareTo(MAX_RPE) > 0)) {
			throw new IllegalArgumentException("actualRpe must be between 0 and 10");
		}
	}

	private static void requireTimestamps(WorkoutExerciseSetStatus status, Instant completedAt) {
		if (status == WorkoutExerciseSetStatus.COMPLETED && completedAt == null) {
			throw new IllegalArgumentException("completedAt is required when status is COMPLETED");
		}
		if (status != WorkoutExerciseSetStatus.COMPLETED && completedAt != null) {
			throw new IllegalArgumentException("completedAt is only allowed when status is COMPLETED");
		}
	}

	private static void requireNonNegative(Integer value, String field) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(field + " must be >= 0");
		}
	}

	private static void requirePairedWeight(BigDecimal weight, WeightUnit unit, String weightField, String unitField) {
		if ((weight == null) != (unit == null)) {
			throw new IllegalArgumentException(
					weightField + " and " + unitField + " must both be provided or both omitted");
		}
		if (weight != null && weight.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException(weightField + " must be >= 0");
		}
	}

	private static void requirePairedDistance(
			BigDecimal distance,
			DistanceUnit unit,
			String distanceField,
			String unitField) {
		if ((distance == null) != (unit == null)) {
			throw new IllegalArgumentException(
					distanceField + " and " + unitField + " must both be provided or both omitted");
		}
		if (distance != null && distance.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException(distanceField + " must be >= 0");
		}
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

	public WorkoutExerciseSetId id() {
		return id;
	}

	public WorkoutExerciseExecutionId workoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int setNumber() {
		return setNumber;
	}

	public int displayOrder() {
		return displayOrder;
	}

	public WorkoutExerciseSetType setType() {
		return setType;
	}

	public Integer prescribedMinimumReps() {
		return prescribedMinimumReps;
	}

	public Integer prescribedMaximumReps() {
		return prescribedMaximumReps;
	}

	public BigDecimal prescribedWeight() {
		return prescribedWeight;
	}

	public WeightUnit prescribedWeightUnit() {
		return prescribedWeightUnit;
	}

	public Integer prescribedDurationSeconds() {
		return prescribedDurationSeconds;
	}

	public BigDecimal prescribedDistance() {
		return prescribedDistance;
	}

	public DistanceUnit prescribedDistanceUnit() {
		return prescribedDistanceUnit;
	}

	public Integer prescribedTargetRpe() {
		return prescribedTargetRpe;
	}

	public Integer prescribedRestSeconds() {
		return prescribedRestSeconds;
	}

	public Integer actualReps() {
		return actualReps;
	}

	public BigDecimal actualWeight() {
		return actualWeight;
	}

	public WeightUnit actualWeightUnit() {
		return actualWeightUnit;
	}

	public Integer actualDurationSeconds() {
		return actualDurationSeconds;
	}

	public BigDecimal actualDistance() {
		return actualDistance;
	}

	public DistanceUnit actualDistanceUnit() {
		return actualDistanceUnit;
	}

	public Integer actualRestSeconds() {
		return actualRestSeconds;
	}

	public BigDecimal actualRpe() {
		return actualRpe;
	}

	public WorkoutExerciseSetStatus status() {
		return status;
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
