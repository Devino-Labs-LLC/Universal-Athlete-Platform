package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One execution row inside a workout adaptation proposal.
 */
public final class WorkoutAdaptationProposalItem implements WorkoutAdaptationProposalStatusResolver.ResolvableItem {

	private static final int MAX_ATHLETE_NOTES_LENGTH = 2000;

	private final WorkoutAdaptationProposalItemId id;
	private final WorkoutAdaptationProposalId proposalId;
	private final WorkoutExerciseExecutionId workoutExerciseExecutionId;
	private final WorkoutExerciseId sourceWorkoutExerciseId;
	private final int executionOrder;
	private final ExerciseDefinitionId prescribedExerciseDefinitionId;
	private final String prescribedNameSnapshot;
	private final ExerciseDefinitionId currentPerformedExerciseDefinitionId;
	private final String currentPerformedNameSnapshot;
	private final ExercisePerformanceKey exercisePerformanceKeyAtGeneration;
	private final boolean currentFeasible;
	private final boolean prescribedFeasible;
	private final boolean performedFeasible;
	private final List<EquipmentType> missingRequiredEquipment;
	private final FeasibilityReasonCode analysisReasonCode;
	private WorkoutAdaptationAction action;
	private final ExerciseDefinitionId generatedTargetExerciseDefinitionId;
	private final String generatedTargetNameSnapshot;
	private final ExerciseSubstitutionRelationshipId generatedRelationshipId;
	private final ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot;
	private final ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot;
	private final String generatedRationaleSnapshot;
	private ExerciseDefinitionId selectedTargetExerciseDefinitionId;
	private ExerciseSubstitutionRelationshipId selectedRelationshipId;
	private WorkoutAdaptationDecision athleteDecision;
	private String athleteNotes;
	private final List<WorkoutAdaptationAlternative> alternatives;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	WorkoutAdaptationProposalItem(
			WorkoutAdaptationProposalItemId id,
			WorkoutAdaptationProposalId proposalId,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			WorkoutExerciseId sourceWorkoutExerciseId,
			int executionOrder,
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			String prescribedNameSnapshot,
			ExerciseDefinitionId currentPerformedExerciseDefinitionId,
			String currentPerformedNameSnapshot,
			ExercisePerformanceKey exercisePerformanceKeyAtGeneration,
			boolean currentFeasible,
			boolean prescribedFeasible,
			boolean performedFeasible,
			List<EquipmentType> missingRequiredEquipment,
			FeasibilityReasonCode analysisReasonCode,
			WorkoutAdaptationAction action,
			ExerciseDefinitionId generatedTargetExerciseDefinitionId,
			String generatedTargetNameSnapshot,
			ExerciseSubstitutionRelationshipId generatedRelationshipId,
			ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot,
			String generatedRationaleSnapshot,
			ExerciseDefinitionId selectedTargetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId selectedRelationshipId,
			WorkoutAdaptationDecision athleteDecision,
			String athleteNotes,
			List<WorkoutAdaptationAlternative> alternatives,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.proposalId = Objects.requireNonNull(proposalId, "proposalId must not be null");
		this.workoutExerciseExecutionId = Objects.requireNonNull(
				workoutExerciseExecutionId, "workoutExerciseExecutionId must not be null");
		this.sourceWorkoutExerciseId = Objects.requireNonNull(
				sourceWorkoutExerciseId, "sourceWorkoutExerciseId must not be null");
		this.executionOrder = executionOrder;
		this.prescribedExerciseDefinitionId = Objects.requireNonNull(
				prescribedExerciseDefinitionId, "prescribedExerciseDefinitionId must not be null");
		this.prescribedNameSnapshot = Objects.requireNonNull(
				prescribedNameSnapshot, "prescribedNameSnapshot must not be null");
		this.currentPerformedExerciseDefinitionId = Objects.requireNonNull(
				currentPerformedExerciseDefinitionId, "currentPerformedExerciseDefinitionId must not be null");
		this.currentPerformedNameSnapshot = Objects.requireNonNull(
				currentPerformedNameSnapshot, "currentPerformedNameSnapshot must not be null");
		this.exercisePerformanceKeyAtGeneration = Objects.requireNonNull(
				exercisePerformanceKeyAtGeneration, "exercisePerformanceKeyAtGeneration must not be null");
		this.currentFeasible = currentFeasible;
		this.prescribedFeasible = prescribedFeasible;
		this.performedFeasible = performedFeasible;
		this.missingRequiredEquipment = missingRequiredEquipment == null
				? List.of()
				: List.copyOf(missingRequiredEquipment);
		this.analysisReasonCode = Objects.requireNonNull(analysisReasonCode, "analysisReasonCode must not be null");
		this.action = Objects.requireNonNull(action, "action must not be null");
		this.generatedTargetExerciseDefinitionId = generatedTargetExerciseDefinitionId;
		this.generatedTargetNameSnapshot = generatedTargetNameSnapshot;
		this.generatedRelationshipId = generatedRelationshipId;
		this.generatedRelationshipTypeSnapshot = generatedRelationshipTypeSnapshot;
		this.generatedCompatibilitySnapshot = generatedCompatibilitySnapshot;
		this.generatedRationaleSnapshot = generatedRationaleSnapshot;
		this.selectedTargetExerciseDefinitionId = selectedTargetExerciseDefinitionId;
		this.selectedRelationshipId = selectedRelationshipId;
		this.athleteDecision = Objects.requireNonNull(athleteDecision, "athleteDecision must not be null");
		this.athleteNotes = normalizeNotes(athleteNotes);
		this.alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutAdaptationProposalItem forGeneration(
			WorkoutAdaptationProposalId proposalId,
			WorkoutExerciseExecution execution,
			boolean currentFeasible,
			boolean prescribedFeasible,
			boolean performedFeasible,
			List<EquipmentType> missingRequiredEquipment,
			FeasibilityReasonCode analysisReasonCode,
			WorkoutAdaptationAction action,
			ExerciseDefinitionId generatedTargetExerciseDefinitionId,
			String generatedTargetNameSnapshot,
			ExerciseSubstitutionRelationshipId generatedRelationshipId,
			ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot,
			String generatedRationaleSnapshot,
			List<WorkoutAdaptationAlternative> alternatives,
			Clock clock) {
		Instant now = Instant.now(clock);
		WorkoutAdaptationDecision decision = action == WorkoutAdaptationAction.NO_CHANGE
				? WorkoutAdaptationDecision.NOT_REQUIRED
				: WorkoutAdaptationDecision.PENDING;
		ExerciseDefinitionId selectedTarget = action == WorkoutAdaptationAction.SUBSTITUTE
				? generatedTargetExerciseDefinitionId
				: null;
		ExerciseSubstitutionRelationshipId selectedRelationship = action == WorkoutAdaptationAction.SUBSTITUTE
				? generatedRelationshipId
				: null;
		return new WorkoutAdaptationProposalItem(
				WorkoutAdaptationProposalItemId.generate(),
				proposalId,
				execution.id(),
				execution.sourceWorkoutExerciseId(),
				execution.displayOrder(),
				execution.prescribedExerciseDefinitionId(),
				execution.prescribedExerciseNameSnapshot(),
				execution.performedExerciseDefinitionId(),
				execution.performedExerciseNameSnapshot(),
				execution.exercisePerformanceKey(),
				currentFeasible,
				prescribedFeasible,
				performedFeasible,
				missingRequiredEquipment,
				analysisReasonCode,
				action,
				generatedTargetExerciseDefinitionId,
				generatedTargetNameSnapshot,
				generatedRelationshipId,
				generatedRelationshipTypeSnapshot,
				generatedCompatibilitySnapshot,
				generatedRationaleSnapshot,
				selectedTarget,
				selectedRelationship,
				decision,
				null,
				alternatives,
				now,
				now,
				0L);
	}

	public static WorkoutAdaptationProposalItem rehydrate(
			WorkoutAdaptationProposalItemId id,
			WorkoutAdaptationProposalId proposalId,
			WorkoutExerciseExecutionId workoutExerciseExecutionId,
			WorkoutExerciseId sourceWorkoutExerciseId,
			int executionOrder,
			ExerciseDefinitionId prescribedExerciseDefinitionId,
			String prescribedNameSnapshot,
			ExerciseDefinitionId currentPerformedExerciseDefinitionId,
			String currentPerformedNameSnapshot,
			ExercisePerformanceKey exercisePerformanceKeyAtGeneration,
			boolean currentFeasible,
			boolean prescribedFeasible,
			boolean performedFeasible,
			List<EquipmentType> missingRequiredEquipment,
			FeasibilityReasonCode analysisReasonCode,
			WorkoutAdaptationAction action,
			ExerciseDefinitionId generatedTargetExerciseDefinitionId,
			String generatedTargetNameSnapshot,
			ExerciseSubstitutionRelationshipId generatedRelationshipId,
			ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot,
			String generatedRationaleSnapshot,
			ExerciseDefinitionId selectedTargetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId selectedRelationshipId,
			WorkoutAdaptationDecision athleteDecision,
			String athleteNotes,
			List<WorkoutAdaptationAlternative> alternatives,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutAdaptationProposalItem(
				id,
				proposalId,
				workoutExerciseExecutionId,
				sourceWorkoutExerciseId,
				executionOrder,
				prescribedExerciseDefinitionId,
				prescribedNameSnapshot,
				currentPerformedExerciseDefinitionId,
				currentPerformedNameSnapshot,
				exercisePerformanceKeyAtGeneration,
				currentFeasible,
				prescribedFeasible,
				performedFeasible,
				missingRequiredEquipment,
				analysisReasonCode,
				action,
				generatedTargetExerciseDefinitionId,
				generatedTargetNameSnapshot,
				generatedRelationshipId,
				generatedRelationshipTypeSnapshot,
				generatedCompatibilitySnapshot,
				generatedRationaleSnapshot,
				selectedTargetExerciseDefinitionId,
				selectedRelationshipId,
				athleteDecision,
				athleteNotes,
				alternatives,
				createdAt,
				updatedAt,
				version);
	}

	public void acceptDefault(Clock clock) {
		requireSubstituteAction();
		if (generatedTargetExerciseDefinitionId == null) {
			throw new IllegalStateException("Generated target is required to accept the default substitute");
		}
		action = WorkoutAdaptationAction.SUBSTITUTE;
		selectedTargetExerciseDefinitionId = generatedTargetExerciseDefinitionId;
		selectedRelationshipId = generatedRelationshipId;
		athleteDecision = WorkoutAdaptationDecision.ACCEPTED;
		touch(clock);
	}

	public void overrideTarget(
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId relationshipId,
			String notes,
			Clock clock) {
		Objects.requireNonNull(targetExerciseDefinitionId, "targetExerciseDefinitionId must not be null");
		if (targetExerciseDefinitionId.equals(currentPerformedExerciseDefinitionId)) {
			throw new IllegalArgumentException("Override target must differ from the current performed exercise");
		}
		requireSubstituteAction();
		action = WorkoutAdaptationAction.SUBSTITUTE;
		selectedTargetExerciseDefinitionId = targetExerciseDefinitionId;
		selectedRelationshipId = relationshipId;
		athleteDecision = WorkoutAdaptationDecision.OVERRIDDEN;
		athleteNotes = normalizeNotes(notes);
		touch(clock);
	}

	public void reject(String notes, Clock clock) {
		if (action == WorkoutAdaptationAction.NO_CHANGE) {
			throw new IllegalStateException("Feasible items cannot be rejected");
		}
		action = WorkoutAdaptationAction.EXCLUDED;
		selectedTargetExerciseDefinitionId = null;
		selectedRelationshipId = null;
		athleteDecision = WorkoutAdaptationDecision.REJECTED;
		athleteNotes = normalizeNotes(notes);
		touch(clock);
	}

	public void resetToPending(Clock clock) {
		if (action == WorkoutAdaptationAction.NO_CHANGE) {
			throw new IllegalStateException("Feasible items cannot be reset");
		}
		if (action == WorkoutAdaptationAction.UNRESOLVED) {
			throw new IllegalStateException("Unresolved items cannot be reset to pending");
		}
		action = generatedTargetExerciseDefinitionId == null
				? WorkoutAdaptationAction.UNRESOLVED
				: WorkoutAdaptationAction.SUBSTITUTE;
		selectedTargetExerciseDefinitionId = generatedTargetExerciseDefinitionId;
		selectedRelationshipId = generatedRelationshipId;
		athleteDecision = WorkoutAdaptationDecision.PENDING;
		athleteNotes = null;
		touch(clock);
	}

	boolean countsTowardExpectedFeasibility() {
		if (currentFeasible || action == WorkoutAdaptationAction.NO_CHANGE) {
			return true;
		}
		if (action == WorkoutAdaptationAction.SUBSTITUTE
				&& (athleteDecision == WorkoutAdaptationDecision.PENDING
						|| athleteDecision == WorkoutAdaptationDecision.ACCEPTED
						|| athleteDecision == WorkoutAdaptationDecision.OVERRIDDEN)) {
			return effectiveTargetExerciseDefinitionId().isPresent();
		}
		return false;
	}

	boolean countsTowardExpectedIfAllAccepted() {
		if (currentFeasible || action == WorkoutAdaptationAction.NO_CHANGE) {
			return true;
		}
		return action == WorkoutAdaptationAction.SUBSTITUTE && generatedTargetExerciseDefinitionId != null;
	}

	boolean countsTowardAcceptedFeasibility() {
		if (currentFeasible || action == WorkoutAdaptationAction.NO_CHANGE) {
			return true;
		}
		return action == WorkoutAdaptationAction.SUBSTITUTE
				&& (athleteDecision == WorkoutAdaptationDecision.ACCEPTED
						|| athleteDecision == WorkoutAdaptationDecision.OVERRIDDEN)
				&& effectiveTargetExerciseDefinitionId().isPresent();
	}

	public boolean shouldApply() {
		return action == WorkoutAdaptationAction.SUBSTITUTE
				&& (athleteDecision == WorkoutAdaptationDecision.ACCEPTED
						|| athleteDecision == WorkoutAdaptationDecision.OVERRIDDEN)
				&& effectiveTargetExerciseDefinitionId().isPresent();
	}

	public Optional<ExerciseDefinitionId> effectiveTargetExerciseDefinitionId() {
		if (selectedTargetExerciseDefinitionId != null) {
			return Optional.of(selectedTargetExerciseDefinitionId);
		}
		return Optional.ofNullable(generatedTargetExerciseDefinitionId);
	}

	public Optional<ExerciseSubstitutionRelationshipId> effectiveRelationshipId() {
		if (selectedRelationshipId != null) {
			return Optional.of(selectedRelationshipId);
		}
		return Optional.ofNullable(generatedRelationshipId);
	}

	private void requireSubstituteAction() {
		if (action != WorkoutAdaptationAction.SUBSTITUTE && action != WorkoutAdaptationAction.EXCLUDED) {
			throw new IllegalStateException("Item action does not support substitute decisions");
		}
	}

	private void touch(Clock clock) {
		updatedAt = Instant.now(clock);
	}

	private static String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String trimmed = notes.trim();
		if (trimmed.length() > MAX_ATHLETE_NOTES_LENGTH) {
			throw new IllegalArgumentException("athleteNotes must not exceed " + MAX_ATHLETE_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	public WorkoutAdaptationProposalItemId id() {
		return id;
	}

	public WorkoutAdaptationProposalId proposalId() {
		return proposalId;
	}

	public WorkoutExerciseExecutionId workoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	public WorkoutExerciseId sourceWorkoutExerciseId() {
		return sourceWorkoutExerciseId;
	}

	public int executionOrder() {
		return executionOrder;
	}

	public ExerciseDefinitionId prescribedExerciseDefinitionId() {
		return prescribedExerciseDefinitionId;
	}

	public String prescribedNameSnapshot() {
		return prescribedNameSnapshot;
	}

	public ExerciseDefinitionId currentPerformedExerciseDefinitionId() {
		return currentPerformedExerciseDefinitionId;
	}

	public String currentPerformedNameSnapshot() {
		return currentPerformedNameSnapshot;
	}

	public ExercisePerformanceKey exercisePerformanceKeyAtGeneration() {
		return exercisePerformanceKeyAtGeneration;
	}

	public boolean currentFeasible() {
		return currentFeasible;
	}

	public boolean prescribedFeasible() {
		return prescribedFeasible;
	}

	public boolean performedFeasible() {
		return performedFeasible;
	}

	public List<EquipmentType> missingRequiredEquipment() {
		return missingRequiredEquipment;
	}

	public FeasibilityReasonCode analysisReasonCode() {
		return analysisReasonCode;
	}

	@Override
	public WorkoutAdaptationAction action() {
		return action;
	}

	public ExerciseDefinitionId generatedTargetExerciseDefinitionId() {
		return generatedTargetExerciseDefinitionId;
	}

	public String generatedTargetNameSnapshot() {
		return generatedTargetNameSnapshot;
	}

	public ExerciseSubstitutionRelationshipId generatedRelationshipId() {
		return generatedRelationshipId;
	}

	public ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot() {
		return generatedRelationshipTypeSnapshot;
	}

	public ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot() {
		return generatedCompatibilitySnapshot;
	}

	public String generatedRationaleSnapshot() {
		return generatedRationaleSnapshot;
	}

	public ExerciseDefinitionId selectedTargetExerciseDefinitionId() {
		return selectedTargetExerciseDefinitionId;
	}

	public ExerciseSubstitutionRelationshipId selectedRelationshipId() {
		return selectedRelationshipId;
	}

	@Override
	public WorkoutAdaptationDecision decision() {
		return athleteDecision;
	}

	public WorkoutAdaptationDecision athleteDecision() {
		return athleteDecision;
	}

	public String athleteNotes() {
		return athleteNotes;
	}

	public List<WorkoutAdaptationAlternative> alternatives() {
		return alternatives;
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
