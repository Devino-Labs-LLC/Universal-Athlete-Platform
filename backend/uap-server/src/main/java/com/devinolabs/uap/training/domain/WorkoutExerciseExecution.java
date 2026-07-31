package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * One movement as it is actually trained on one occurrence.
 *
 * <p>The execution carries two identities. The prescribed one is snapshotted from the plan at
 * generation time and never changes, so the plan can always be compared with what happened. The
 * performed one starts equal to it and moves when the athlete substitutes another movement; it is
 * the identity results are aggregated under, which is why {@link #exercisePerformanceKey()} is
 * always derived from it.
 */
public class WorkoutExerciseExecution {

	private static final int MAX_ATHLETE_NOTES_LENGTH = 4000;
	private static final int MAX_SUBSTITUTION_NOTES_LENGTH = 2000;

	private final WorkoutExerciseExecutionId id;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final WorkoutExerciseId sourceWorkoutExerciseId;
	private final ExerciseDefinitionId prescribedExerciseDefinitionId;
	private final String prescribedExerciseNameSnapshot;
	private ExerciseDefinitionId performedExerciseDefinitionId;
	private String performedExerciseNameSnapshot;
	private ExerciseDefinitionCategory performedExerciseCategorySnapshot;
	private MovementPattern performedPrimaryMovementPatternSnapshot;
	private ImpactLevel performedImpactLevelSnapshot;
	private ExercisePerformanceKey exercisePerformanceKey;
	private ExerciseSubstitutionReason substitutionReason;
	private String substitutionNotes;
	private Instant substitutedAt;
	private final AthleteId athleteId;
	private final int displayOrder;
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
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			String prescribedExerciseNameSnapshot,
			ExerciseDefinitionId performedExerciseDefinitionId,
			String performedExerciseNameSnapshot,
			ExerciseDefinitionCategory performedExerciseCategorySnapshot,
			MovementPattern performedPrimaryMovementPatternSnapshot,
			ImpactLevel performedImpactLevelSnapshot,
			ExercisePerformanceKey exercisePerformanceKey,
			ExerciseSubstitutionReason substitutionReason,
			String substitutionNotes,
			Instant substitutedAt,
			AthleteId athleteId,
			int displayOrder,
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
		this.prescribedExerciseDefinitionId = Objects.requireNonNull(
				prescribedExerciseDefinitionId, "prescribedExerciseDefinitionId must not be null");
		this.prescribedExerciseNameSnapshot = Objects.requireNonNull(
				prescribedExerciseNameSnapshot, "prescribedExerciseNameSnapshot must not be null");
		this.performedExerciseDefinitionId = Objects.requireNonNull(
				performedExerciseDefinitionId, "performedExerciseDefinitionId must not be null");
		this.performedExerciseNameSnapshot = Objects.requireNonNull(
				performedExerciseNameSnapshot, "performedExerciseNameSnapshot must not be null");
		this.performedExerciseCategorySnapshot = Objects.requireNonNull(
				performedExerciseCategorySnapshot, "performedExerciseCategorySnapshot must not be null");
		this.performedPrimaryMovementPatternSnapshot = Objects.requireNonNull(
				performedPrimaryMovementPatternSnapshot, "performedPrimaryMovementPatternSnapshot must not be null");
		this.performedImpactLevelSnapshot = Objects.requireNonNull(
				performedImpactLevelSnapshot, "performedImpactLevelSnapshot must not be null");
		this.exercisePerformanceKey = Objects.requireNonNull(
				exercisePerformanceKey, "exercisePerformanceKey must not be null");
		requirePerformedIdentity(this.performedExerciseDefinitionId, this.exercisePerformanceKey);
		this.substitutionReason = substitutionReason;
		this.substitutionNotes = normalizeSubstitutionNotes(substitutionNotes);
		this.substitutedAt = substitutedAt;
		requireSubstitutionState(
				this.prescribedExerciseDefinitionId,
				this.performedExerciseDefinitionId,
				this.substitutionReason,
				this.substitutionNotes,
				this.substitutedAt);
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
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
			ExerciseDefinition exerciseDefinition,
			WorkoutOccurrenceId occurrenceId,
			Clock clock) {
		Objects.requireNonNull(exercise, "exercise must not be null");
		Objects.requireNonNull(exerciseDefinition, "exerciseDefinition must not be null");
		Objects.requireNonNull(occurrenceId, "occurrenceId must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		ExerciseDefinitionMetadata metadata = exerciseDefinition.metadata();
		return new WorkoutExerciseExecution(
				WorkoutExerciseExecutionId.generate(),
				occurrenceId,
				exercise.id(),
				exercise.exerciseDefinitionId(),
				exercise.exerciseName(),
				exercise.exerciseDefinitionId(),
				exercise.exerciseName(),
				metadata.category(),
				metadata.primaryMovementPattern(),
				metadata.impactLevel(),
				ExercisePerformanceKey.of(exercise.exerciseDefinitionId()),
				null,
				null,
				null,
				exercise.athleteId(),
				exercise.displayOrder(),
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
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			String prescribedExerciseNameSnapshot,
			ExerciseDefinitionId performedExerciseDefinitionId,
			String performedExerciseNameSnapshot,
			ExerciseDefinitionCategory performedExerciseCategorySnapshot,
			MovementPattern performedPrimaryMovementPatternSnapshot,
			ImpactLevel performedImpactLevelSnapshot,
			ExercisePerformanceKey exercisePerformanceKey,
			ExerciseSubstitutionReason substitutionReason,
			String substitutionNotes,
			Instant substitutedAt,
			AthleteId athleteId,
			int displayOrder,
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
				prescribedExerciseDefinitionId,
				prescribedExerciseNameSnapshot,
				performedExerciseDefinitionId,
				performedExerciseNameSnapshot,
				performedExerciseCategorySnapshot,
				performedPrimaryMovementPatternSnapshot,
				performedImpactLevelSnapshot,
				exercisePerformanceKey,
				substitutionReason,
				substitutionNotes,
				substitutedAt,
				athleteId,
				displayOrder,
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

	/**
	 * Records that another movement is performed in place of the prescribed one.
	 *
	 * <p>Only the performed identity moves: the prescription and every prescribed snapshot stay as
	 * the plan wrote them, so the deviation is visible rather than hidden. Substituting back to the
	 * prescribed movement is accepted and simply leaves the execution unsubstituted again.
	 */
	public void substitute(
			ExerciseDefinition target,
			ExerciseSubstitutionReason reason,
			String notes,
			Clock clock) {
		Objects.requireNonNull(target, "target must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (reason == null) {
			throw new InvalidExerciseSubstitutionReasonException("substitutionReason is required");
		}
		if (reason == ExerciseSubstitutionReason.REVERSION) {
			throw new InvalidExerciseSubstitutionReasonException(
					"REVERSION is reserved for undoing a substitution");
		}
		if (target.id().equals(performedExerciseDefinitionId)) {
			throw new WorkoutExerciseAlreadyUsesDefinitionException();
		}
		String normalizedNotes = normalizeSubstitutionNotes(notes);
		boolean backToPrescription = target.id().equals(prescribedExerciseDefinitionId);
		this.performedExerciseDefinitionId = target.id();
		this.performedExerciseNameSnapshot = target.canonicalName();
		this.performedExerciseCategorySnapshot = target.metadata().category();
		this.performedPrimaryMovementPatternSnapshot = target.metadata().primaryMovementPattern();
		this.performedImpactLevelSnapshot = target.metadata().impactLevel();
		this.exercisePerformanceKey = ExercisePerformanceKey.of(target.id());
		this.substitutionReason = backToPrescription ? null : reason;
		this.substitutionNotes = backToPrescription ? null : normalizedNotes;
		this.substitutedAt = backToPrescription ? null : Instant.now(clock);
		touch(clock);
	}

	/**
	 * Puts the execution back on the prescribed movement. The substitution itself is not erased: the
	 * caller appends the reversion to the substitution log before this returns.
	 */
	public void revertSubstitution(ExerciseDefinition prescribedDefinition, Clock clock) {
		Objects.requireNonNull(prescribedDefinition, "prescribedDefinition must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!isSubstituted()) {
			throw new WorkoutExerciseNotSubstitutedException();
		}
		ExerciseDefinitionMetadata metadata = prescribedDefinition.metadata();
		this.performedExerciseDefinitionId = prescribedExerciseDefinitionId;
		this.performedExerciseNameSnapshot = prescribedExerciseNameSnapshot;
		this.performedExerciseCategorySnapshot = metadata.category();
		this.performedPrimaryMovementPatternSnapshot = metadata.primaryMovementPattern();
		this.performedImpactLevelSnapshot = metadata.impactLevel();
		this.exercisePerformanceKey = ExercisePerformanceKey.of(prescribedExerciseDefinitionId);
		this.substitutionReason = null;
		this.substitutionNotes = null;
		this.substitutedAt = null;
		touch(clock);
	}

	public boolean isSubstituted() {
		return !performedExerciseDefinitionId.equals(prescribedExerciseDefinitionId);
	}

	/**
	 * Substitution changes what the logged results mean, so it is only offered while the execution
	 * has not yet reached a terminal state. Callers additionally require every set to be untouched.
	 */
	public boolean isSubstitutable() {
		return status == WorkoutExerciseExecutionStatus.NOT_STARTED
				|| status == WorkoutExerciseExecutionStatus.IN_PROGRESS;
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

	private static void requirePerformedIdentity(
			ExerciseDefinitionId performedExerciseDefinitionId,
			ExercisePerformanceKey exercisePerformanceKey) {
		if (!exercisePerformanceKey.equals(ExercisePerformanceKey.of(performedExerciseDefinitionId))) {
			throw new ExercisePerformanceIdentityConflictException(
					"exercisePerformanceKey must be derived from performedExerciseDefinitionId");
		}
	}

	private static void requireSubstitutionState(
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			ExerciseDefinitionId performedExerciseDefinitionId,
			ExerciseSubstitutionReason substitutionReason,
			String substitutionNotes,
			Instant substitutedAt) {
		boolean substituted = !performedExerciseDefinitionId.equals(prescribedExerciseDefinitionId);
		if (substituted && (substitutionReason == null || substitutedAt == null)) {
			throw new WorkoutExerciseSubstitutionIdentityConflictException(
					"A substituted execution requires a substitution reason and timestamp");
		}
		if (!substituted && (substitutionReason != null || substitutionNotes != null || substitutedAt != null)) {
			throw new WorkoutExerciseSubstitutionIdentityConflictException(
					"Substitution details are only allowed while an execution performs a substitute movement");
		}
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

	static String normalizeSubstitutionNotes(String substitutionNotes) {
		if (substitutionNotes == null || substitutionNotes.isBlank()) {
			return null;
		}
		String trimmed = substitutionNotes.trim();
		if (trimmed.length() > MAX_SUBSTITUTION_NOTES_LENGTH) {
			throw new IllegalArgumentException(
					"substitutionNotes must not exceed " + MAX_SUBSTITUTION_NOTES_LENGTH + " characters");
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

	/**
	 * Canonical movement the plan asked for, snapshotted at generation time and never rewritten.
	 */
	public ExerciseDefinitionId prescribedExerciseDefinitionId() {
		return prescribedExerciseDefinitionId;
	}

	public String prescribedExerciseNameSnapshot() {
		return prescribedExerciseNameSnapshot;
	}

	/**
	 * Canonical movement actually trained; equal to the prescribed one until a substitution.
	 */
	public ExerciseDefinitionId performedExerciseDefinitionId() {
		return performedExerciseDefinitionId;
	}

	public String performedExerciseNameSnapshot() {
		return performedExerciseNameSnapshot;
	}

	public ExerciseDefinitionCategory performedExerciseCategorySnapshot() {
		return performedExerciseCategorySnapshot;
	}

	public MovementPattern performedPrimaryMovementPatternSnapshot() {
		return performedPrimaryMovementPatternSnapshot;
	}

	public ImpactLevel performedImpactLevelSnapshot() {
		return performedImpactLevelSnapshot;
	}

	/**
	 * Convenience alias for {@link #performedExerciseNameSnapshot()}: results are described by what
	 * the athlete actually did.
	 */
	public String exerciseName() {
		return performedExerciseNameSnapshot;
	}

	/**
	 * Identity this execution's results are aggregated under for history and personal records,
	 * always derived from {@link #performedExerciseDefinitionId()}.
	 */
	public ExercisePerformanceKey exercisePerformanceKey() {
		return exercisePerformanceKey;
	}

	public ExerciseSubstitutionReason substitutionReason() {
		return substitutionReason;
	}

	public String substitutionNotes() {
		return substitutionNotes;
	}

	public Instant substitutedAt() {
		return substitutedAt;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int displayOrder() {
		return displayOrder;
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
