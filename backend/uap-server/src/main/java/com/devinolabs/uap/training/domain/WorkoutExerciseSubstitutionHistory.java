package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Append-only log of every movement change an athlete made on one execution.
 *
 * <p>Entries are never rewritten or deleted: substituting and undoing a substitution both append a
 * row, so the sequence read in {@code changedAt} order replays exactly what was trained and why.
 * Names are snapshotted on both sides because a definition can be renamed long after the session.
 */
public class WorkoutExerciseSubstitutionHistory {

	private final WorkoutExerciseSubstitutionHistoryId id;
	private final AthleteId athleteId;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final WorkoutExerciseExecutionId workoutExerciseExecutionId;
	private final ExerciseDefinitionId fromExerciseDefinitionId;
	private final String fromExerciseNameSnapshot;
	private final ExerciseDefinitionId toExerciseDefinitionId;
	private final String toExerciseNameSnapshot;
	private final ExerciseSubstitutionReason reason;
	private final String notes;
	private final ExerciseSubstitutionRelationshipId substitutionRelationshipId;
	private final ExerciseSubstitutionRelationshipType relationshipTypeSnapshot;
	private final ExerciseSubstitutionCompatibility compatibilitySnapshot;
	private final TrainingEnvironmentId trainingEnvironmentId;
	private final String trainingEnvironmentNameSnapshot;
	private final List<EquipmentType> availableEquipmentSnapshot;
	private final WorkoutAdaptationProposalId workoutAdaptationProposalId;
	private final WorkoutAdaptationProposalItemId workoutAdaptationProposalItemId;
	private final WorkoutAdaptationDecision adaptationDecisionSnapshot;
	private final boolean reverted;
	private final Instant changedAt;
	private final Instant createdAt;

	private WorkoutExerciseSubstitutionHistory(
			WorkoutExerciseSubstitutionHistoryId id,
			AthleteId athleteId,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseDefinitionId toExerciseDefinitionId,
			String toExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId,
			ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility compatibilitySnapshot,
			TrainingEnvironmentId trainingEnvironmentId,
			String trainingEnvironmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			WorkoutAdaptationProposalId workoutAdaptationProposalId,
			WorkoutAdaptationProposalItemId workoutAdaptationProposalItemId,
			WorkoutAdaptationDecision adaptationDecisionSnapshot,
			boolean reverted,
			Instant changedAt,
			Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(
				workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.workoutExerciseExecutionId = Objects.requireNonNull(
				workoutExerciseExecutionId, "workoutExerciseExecutionId must not be null");
		this.fromExerciseDefinitionId = Objects.requireNonNull(
				fromExerciseDefinitionId, "fromExerciseDefinitionId must not be null");
		this.fromExerciseNameSnapshot = Objects.requireNonNull(
				fromExerciseNameSnapshot, "fromExerciseNameSnapshot must not be null");
		this.toExerciseDefinitionId = Objects.requireNonNull(
				toExerciseDefinitionId, "toExerciseDefinitionId must not be null");
		this.toExerciseNameSnapshot = Objects.requireNonNull(
				toExerciseNameSnapshot, "toExerciseNameSnapshot must not be null");
		if (fromExerciseDefinitionId.equals(toExerciseDefinitionId)) {
			throw new WorkoutExerciseAlreadyUsesDefinitionException(
					"A substitution entry must change the performed exercise definition");
		}
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
		this.notes = WorkoutExerciseExecution.normalizeSubstitutionNotes(notes);
		this.substitutionRelationshipId = substitutionRelationshipId;
		this.relationshipTypeSnapshot = relationshipTypeSnapshot;
		this.compatibilitySnapshot = compatibilitySnapshot;
		this.trainingEnvironmentId = trainingEnvironmentId;
		this.trainingEnvironmentNameSnapshot = trainingEnvironmentNameSnapshot;
		this.availableEquipmentSnapshot = availableEquipmentSnapshot == null
				? List.of()
				: List.copyOf(availableEquipmentSnapshot);
		this.workoutAdaptationProposalId = workoutAdaptationProposalId;
		this.workoutAdaptationProposalItemId = workoutAdaptationProposalItemId;
		this.adaptationDecisionSnapshot = adaptationDecisionSnapshot;
		this.reverted = reverted;
		this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	/**
	 * Records a move to a substitute movement without catalogue relationship provenance.
	 */
	public static WorkoutExerciseSubstitutionHistory substitution(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			Clock clock) {
		return substitution(
				execution, fromExerciseDefinitionId, fromExerciseNameSnapshot, reason, notes, null, null, clock);
	}

	public static WorkoutExerciseSubstitutionHistory substitution(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationship relationship,
			Clock clock) {
		return substitution(
				execution, fromExerciseDefinitionId, fromExerciseNameSnapshot, reason, notes, relationship, null, clock);
	}

	public static WorkoutExerciseSubstitutionHistory substitution(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationship relationship,
			WorkoutOccurrenceEnvironmentSnapshot environmentContext,
			Clock clock) {
		return substitutionWithAdaptationProvenance(
				execution,
				fromExerciseDefinitionId,
				fromExerciseNameSnapshot,
				reason,
				notes,
				relationship,
				environmentContext,
				null,
				null,
				null,
				clock);
	}

	public static WorkoutExerciseSubstitutionHistory substitutionWithAdaptationProvenance(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationship relationship,
			WorkoutOccurrenceEnvironmentSnapshot environmentContext,
			WorkoutAdaptationProposalId workoutAdaptationProposalId,
			WorkoutAdaptationProposalItemId workoutAdaptationProposalItemId,
			WorkoutAdaptationDecision adaptationDecisionSnapshot,
			Clock clock) {
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		ExerciseSubstitutionRelationshipId relationshipId = null;
		ExerciseSubstitutionRelationshipType typeSnapshot = null;
		ExerciseSubstitutionCompatibility compatibilitySnapshot = null;
		if (relationship != null) {
			relationshipId = relationship.id();
			typeSnapshot = relationship.relationshipType();
			compatibilitySnapshot = relationship.compatibilityLevel();
		}
		TrainingEnvironmentId environmentId = null;
		String environmentName = null;
		List<EquipmentType> environmentEquipment = List.of();
		if (environmentContext != null) {
			environmentId = environmentContext.trainingEnvironmentId();
			environmentName = environmentContext.nameSnapshot();
			environmentEquipment = environmentContext.availableEquipmentSnapshot();
		}
		return new WorkoutExerciseSubstitutionHistory(
				WorkoutExerciseSubstitutionHistoryId.generate(),
				execution.athleteId(),
				execution.workoutOccurrenceId(),
				execution.id(),
				fromExerciseDefinitionId,
				fromExerciseNameSnapshot,
				execution.performedExerciseDefinitionId(),
				execution.performedExerciseNameSnapshot(),
				reason,
				notes,
				relationshipId,
				typeSnapshot,
				compatibilitySnapshot,
				environmentId,
				environmentName,
				environmentEquipment,
				workoutAdaptationProposalId,
				workoutAdaptationProposalItemId,
				adaptationDecisionSnapshot,
				execution.performedExerciseDefinitionId().equals(execution.prescribedExerciseDefinitionId()),
				now,
				now);
	}

	/**
	 * Records undoing a substitution. The reversion is itself an event in the log, flagged so the
	 * sequence distinguishes "went back to the plan" from "swapped again".
	 */
	public static WorkoutExerciseSubstitutionHistory reversion(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			String notes,
			Clock clock) {
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutExerciseSubstitutionHistory(
				WorkoutExerciseSubstitutionHistoryId.generate(),
				execution.athleteId(),
				execution.workoutOccurrenceId(),
				execution.id(),
				fromExerciseDefinitionId,
				fromExerciseNameSnapshot,
				execution.prescribedExerciseDefinitionId(),
				execution.prescribedExerciseNameSnapshot(),
				ExerciseSubstitutionReason.REVERSION,
				notes,
				null,
				null,
				null,
				null,
				null,
				List.of(),
				null,
				null,
				null,
				true,
				now,
				now);
	}

	public static WorkoutExerciseSubstitutionHistory rehydrate(
			WorkoutExerciseSubstitutionHistoryId id,
			AthleteId athleteId,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			ExerciseDefinitionId fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			ExerciseDefinitionId toExerciseDefinitionId,
			String toExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId,
			ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility compatibilitySnapshot,
			TrainingEnvironmentId trainingEnvironmentId,
			String trainingEnvironmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			WorkoutAdaptationProposalId workoutAdaptationProposalId,
			WorkoutAdaptationProposalItemId workoutAdaptationProposalItemId,
			WorkoutAdaptationDecision adaptationDecisionSnapshot,
			boolean reverted,
			Instant changedAt,
			Instant createdAt) {
		return new WorkoutExerciseSubstitutionHistory(
				id,
				athleteId,
				workoutOccurrenceId,
				workoutExerciseExecutionId,
				fromExerciseDefinitionId,
				fromExerciseNameSnapshot,
				toExerciseDefinitionId,
				toExerciseNameSnapshot,
				reason,
				notes,
				substitutionRelationshipId,
				relationshipTypeSnapshot,
				compatibilitySnapshot,
				trainingEnvironmentId,
				trainingEnvironmentNameSnapshot,
				availableEquipmentSnapshot,
				workoutAdaptationProposalId,
				workoutAdaptationProposalItemId,
				adaptationDecisionSnapshot,
				reverted,
				changedAt,
				createdAt);
	}

	public WorkoutExerciseSubstitutionHistoryId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public WorkoutExerciseExecutionId workoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	public ExerciseDefinitionId fromExerciseDefinitionId() {
		return fromExerciseDefinitionId;
	}

	public String fromExerciseNameSnapshot() {
		return fromExerciseNameSnapshot;
	}

	public ExerciseDefinitionId toExerciseDefinitionId() {
		return toExerciseDefinitionId;
	}

	public String toExerciseNameSnapshot() {
		return toExerciseNameSnapshot;
	}

	public ExerciseSubstitutionReason reason() {
		return reason;
	}

	public String notes() {
		return notes;
	}

	public ExerciseSubstitutionRelationshipId substitutionRelationshipId() {
		return substitutionRelationshipId;
	}

	public ExerciseSubstitutionRelationshipType relationshipTypeSnapshot() {
		return relationshipTypeSnapshot;
	}

	public ExerciseSubstitutionCompatibility compatibilitySnapshot() {
		return compatibilitySnapshot;
	}

	public TrainingEnvironmentId trainingEnvironmentId() {
		return trainingEnvironmentId;
	}

	public String trainingEnvironmentNameSnapshot() {
		return trainingEnvironmentNameSnapshot;
	}

	public List<EquipmentType> availableEquipmentSnapshot() {
		return availableEquipmentSnapshot;
	}

	public WorkoutAdaptationProposalId workoutAdaptationProposalId() {
		return workoutAdaptationProposalId;
	}

	public WorkoutAdaptationProposalItemId workoutAdaptationProposalItemId() {
		return workoutAdaptationProposalItemId;
	}

	public WorkoutAdaptationDecision adaptationDecisionSnapshot() {
		return adaptationDecisionSnapshot;
	}

	public boolean reverted() {
		return reverted;
	}

	public Instant changedAt() {
		return changedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
