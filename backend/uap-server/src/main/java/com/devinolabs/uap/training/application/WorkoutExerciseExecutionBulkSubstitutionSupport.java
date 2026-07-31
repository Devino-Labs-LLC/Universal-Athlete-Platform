package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;

/**
 * Applies selected proposal substitutions in-process without nested transactional use-case calls.
 */
final class WorkoutExerciseExecutionBulkSubstitutionSupport {

	record AppliedSubstitution(
			WorkoutAdaptationProposalItem item,
			WorkoutExerciseExecution execution,
			WorkoutExerciseSubstitutionHistory history) {
	}

	private WorkoutExerciseExecutionBulkSubstitutionSupport() {
	}

	static void requireSelectedSubstitutionsSubstitutable(
			WorkoutAdaptationProposal proposal,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets) {
		Objects.requireNonNull(proposal, "proposal must not be null");
		for (WorkoutAdaptationProposalItem item : proposal.items()) {
			if (!item.shouldApply()) {
				continue;
			}
			WorkoutExerciseExecution execution = executions.stream()
					.filter(candidate -> candidate.id().equals(item.workoutExerciseExecutionId()))
					.findFirst()
					.orElseThrow(WorkoutExerciseExecutionNotFoundException::new);
			List<WorkoutExerciseSet> executionSets = sets.stream()
					.filter(set -> set.workoutExerciseExecutionId().equals(execution.id()))
					.toList();
			WorkoutExerciseExecutionSupport.requireSubstitutable(execution, executionSets);
		}
	}

	static List<AppliedSubstitution> applySelectedSubstitutions(
			WorkoutAdaptationProposal proposal,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			WorkoutExerciseExecutionRepository executionRepository,
			WorkoutExerciseSubstitutionHistoryRepository historyRepository,
			WorkoutOccurrence occurrence,
			AthleteId athleteId,
			Clock clock) {
		Objects.requireNonNull(proposal, "proposal must not be null");
		List<AppliedSubstitution> applied = new ArrayList<>();
		WorkoutOccurrenceEnvironmentSnapshot environmentSnapshot =
				WorkoutAdaptationProposalSupport.resolveEnvironmentSnapshot(occurrence);
		for (WorkoutAdaptationProposalItem item : proposal.items()) {
			if (!item.shouldApply()) {
				continue;
			}
			WorkoutExerciseExecution execution = executions.stream()
					.filter(candidate -> candidate.id().equals(item.workoutExerciseExecutionId()))
					.findFirst()
					.orElseThrow(WorkoutExerciseExecutionNotFoundException::new);
			List<WorkoutExerciseSet> executionSets = sets.stream()
					.filter(set -> set.workoutExerciseExecutionId().equals(execution.id()))
					.toList();
			WorkoutExerciseExecutionSupport.requireSubstitutable(execution, executionSets);
			ExerciseDefinitionId targetId = item.effectiveTargetExerciseDefinitionId()
					.orElseThrow(() -> new IllegalStateException("Selected target is required"));
			ExerciseDefinition target = ExerciseDefinitionAccessPolicy.requireSelectable(
					athleteId,
					ExerciseDefinitionSupport.requireAccessible(exerciseDefinitionRepository, athleteId, targetId));
			WorkoutAdaptationProposalSupport.validateTargetEnvironmentCompatible(
					target, proposal.availableEquipmentSnapshot());
			ExerciseSubstitutionRelationship relationship = resolveRelationship(
					athleteId,
					execution,
					target.id(),
					item.effectiveRelationshipId().orElse(null),
					relationshipRepository,
					exerciseDefinitionRepository);
			ExerciseDefinitionId previousDefinitionId = execution.performedExerciseDefinitionId();
			String previousName = execution.performedExerciseNameSnapshot();
			String notes = item.athleteNotes();
			execution.substitute(target, ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, notes, clock);
			WorkoutExerciseExecution saved = executionRepository.save(execution);
			WorkoutExerciseSubstitutionHistory history = historyRepository.append(
					WorkoutExerciseSubstitutionHistory.substitutionWithAdaptationProvenance(
							saved,
							previousDefinitionId,
							previousName,
							ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE,
							notes,
							relationship,
							environmentSnapshot,
							proposal.id(),
							item.id(),
							item.athleteDecision(),
							clock));
			applied.add(new AppliedSubstitution(item, saved, history));
		}
		return List.copyOf(applied);
	}

	private static ExerciseSubstitutionRelationship resolveRelationship(
			AthleteId athleteId,
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository) {
		if (substitutionRelationshipId == null) {
			return null;
		}
		ExerciseSubstitutionRelationship relationship = relationshipRepository
				.findActiveById(substitutionRelationshipId)
				.orElseThrow(ExerciseSubstitutionRelationshipNotFoundException::new);
		if (!ExerciseSubstitutionRelationshipAccessPolicy.isAccessible(athleteId, relationship)) {
			throw new ExerciseSubstitutionRelationshipNotAccessibleException();
		}
		if (!relationship.sourceExerciseDefinitionId().equals(execution.performedExerciseDefinitionId())) {
			throw new AdaptationRelationshipMismatchException(
					"Substitution relationship source must match the currently performed exercise");
		}
		if (!relationship.targetExerciseDefinitionId().equals(targetExerciseDefinitionId)) {
			throw new AdaptationRelationshipMismatchException(
					"Substitution relationship target must match the requested substitute exercise");
		}
		ExerciseDefinitionAccessPolicy.requireSelectable(
				athleteId,
				ExerciseDefinitionSupport.requireAccessible(
						exerciseDefinitionRepository,
						athleteId,
						relationship.targetExerciseDefinitionId()));
		return relationship;
	}

}
