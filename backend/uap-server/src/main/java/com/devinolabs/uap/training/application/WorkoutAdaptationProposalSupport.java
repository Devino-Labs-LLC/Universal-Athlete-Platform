package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternative;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternativeId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationFeasibilityFingerprint;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;

final class WorkoutAdaptationProposalSupport {

	private WorkoutAdaptationProposalSupport() {
	}

	static WorkoutAdaptationProposal requireOwnedProposal(
			WorkoutAdaptationProposalRepository repository,
			WorkoutAdaptationProposalId proposalId,
			AthleteId athleteId) {
		return repository.findOwnedById(proposalId, athleteId)
				.orElseThrow(WorkoutAdaptationProposalNotFoundException::new);
	}

	static WorkoutAdaptationProposal requireOwnedMutableProposal(
			WorkoutAdaptationProposalRepository repository,
			WorkoutAdaptationProposalId proposalId,
			AthleteId athleteId,
			Clock clock) {
		WorkoutAdaptationProposal proposal = requireOwnedProposal(repository, proposalId, athleteId);
		expireIfNeeded(repository, proposal, clock);
		if (!proposal.status().mutable()) {
			throw terminalException(proposal.status());
		}
		return proposal;
	}

	static WorkoutAdaptationProposal expireIfNeeded(
			WorkoutAdaptationProposalRepository repository,
			WorkoutAdaptationProposal proposal,
			Clock clock) {
		if (proposal.expireIfNeeded(clock)) {
			return repository.save(proposal);
		}
		if (proposal.status() == WorkoutAdaptationProposalStatus.EXPIRED) {
			throw new WorkoutAdaptationProposalExpiredException();
		}
		return proposal;
	}

	static RuntimeException terminalException(WorkoutAdaptationProposalStatus status) {
		if (status == WorkoutAdaptationProposalStatus.EXPIRED) {
			return new WorkoutAdaptationProposalExpiredException();
		}
		if (status == WorkoutAdaptationProposalStatus.STALE) {
			return new WorkoutAdaptationProposalStaleException();
		}
		return new WorkoutAdaptationProposalTerminalException();
	}

	static void requireSubstitutableOccurrence(
			WorkoutOccurrence occurrence,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets) {
		WorkoutExerciseExecutionSupport.requireExecutionWritable(occurrence);
		for (WorkoutExerciseExecution execution : executions) {
			List<WorkoutExerciseSet> executionSets = sets.stream()
					.filter(set -> set.workoutExerciseExecutionId().equals(execution.id()))
					.toList();
			WorkoutExerciseExecutionSupport.requireSubstitutable(execution, executionSets);
		}
	}

	static void requireEnvironmentContext(FeasibilityEnvironmentContextResult environmentContext) {
		if (environmentContext == null) {
			throw new WorkoutAdaptationProposalEnvironmentRequiredException();
		}
	}

	static int resolveExpirationMinutes(Integer expirationMinutes) {
		int resolved = expirationMinutes == null
				? WorkoutAdaptationProposal.DEFAULT_EXPIRATION_MINUTES
				: expirationMinutes;
		try {
			WorkoutAdaptationProposal.validateExpirationMinutes(resolved);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidAdaptationProposalExpirationException();
		}
		return resolved;
	}

	static WorkoutAdaptationFeasibilityFingerprint buildFingerprint(
			WorkoutOccurrence occurrence,
			FeasibilityEnvironmentContextResult environmentContext,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets,
			WorkoutAdaptationProposal proposal,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			AthleteId athleteId) {
		List<WorkoutAdaptationProposalItem> items = proposal == null ? List.of() : proposal.items();
		return buildFingerprint(
				occurrence,
				environmentContext,
				executions,
				sets,
				items,
				definitionsById,
				relationshipRepository,
				athleteId);
	}

	static WorkoutAdaptationFeasibilityFingerprint buildFingerprint(
			WorkoutOccurrence occurrence,
			FeasibilityEnvironmentContextResult environmentContext,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets,
			List<WorkoutAdaptationProposalItem> proposalItems,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			AthleteId athleteId) {
		FeasibilityEnvironmentContextSource source = environmentContext == null
				? FeasibilityEnvironmentContextSource.NONE
				: environmentContext.contextSource();
		TrainingEnvironmentId environmentId = environmentContext == null ? null : environmentContext.trainingEnvironmentId();
		List<com.devinolabs.uap.training.domain.EquipmentType> equipment = environmentContext == null
				? List.of()
				: environmentContext.availableEquipment();
		List<WorkoutAdaptationFeasibilityFingerprint.FingerprintExecution> fingerprintExecutions = new ArrayList<>();
		for (WorkoutExerciseExecution execution : executions) {
			List<WorkoutAdaptationFeasibilityFingerprint.FingerprintSet> fingerprintSets = sets.stream()
					.filter(set -> set.workoutExerciseExecutionId().equals(execution.id()))
					.map(set -> new WorkoutAdaptationFeasibilityFingerprint.FingerprintSet(set.id(), set.status()))
					.toList();
			fingerprintExecutions.add(new WorkoutAdaptationFeasibilityFingerprint.FingerprintExecution(
					execution.id(),
					execution.version(),
					execution.performedExerciseDefinitionId(),
					execution.status(),
					fingerprintSets));
		}
		Map<ExerciseSubstitutionRelationshipId, Boolean> relationshipFlags = new LinkedHashMap<>();
		Map<ExerciseDefinitionId, Boolean> targetFlags = new LinkedHashMap<>();
		for (WorkoutAdaptationProposalItem item : proposalItems) {
			collectTargetFlag(item.generatedTargetExerciseDefinitionId(), definitionsById, targetFlags);
			collectTargetFlag(item.selectedTargetExerciseDefinitionId(), definitionsById, targetFlags);
			collectRelationshipFlag(item.generatedRelationshipId(), relationshipRepository, athleteId, relationshipFlags);
			collectRelationshipFlag(item.selectedRelationshipId(), relationshipRepository, athleteId, relationshipFlags);
			for (WorkoutAdaptationAlternative alternative : item.alternatives()) {
				collectTargetFlag(alternative.targetExerciseDefinitionId(), definitionsById, targetFlags);
				collectRelationshipFlag(alternative.relationshipId(), relationshipRepository, athleteId, relationshipFlags);
			}
		}
		return WorkoutAdaptationFeasibilityFingerprint.compute(
				new WorkoutAdaptationFeasibilityFingerprint.FingerprintInput(
						occurrence.id(),
						occurrence.version(),
						occurrence.status(),
						source,
						environmentId,
						equipment,
						fingerprintExecutions,
						relationshipFlags,
						targetFlags));
	}

	static void validateTargetEnvironmentCompatible(
			ExerciseDefinition target,
			List<com.devinolabs.uap.training.domain.EquipmentType> availableEquipment) {
		if (!ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				target.metadata().requiredEquipment(), availableEquipment).compatible()) {
			throw new AdaptationTargetNotEnvironmentCompatibleException();
		}
	}

	static WorkoutAdaptationProposalResult toResult(WorkoutAdaptationProposal proposal) {
		List<WorkoutAdaptationProposalItemResult> items = proposal.itemsInOrder().stream()
				.map(WorkoutAdaptationProposalSupport::toItemResult)
				.toList();
		return new WorkoutAdaptationProposalResult(
				proposal.id(),
				proposal.athleteId(),
				proposal.trainingPlanId(),
				proposal.workoutDayId(),
				proposal.workoutOccurrenceId(),
				new WorkoutAdaptationEnvironmentContextResult(
						proposal.environmentContextSource(),
						proposal.trainingEnvironmentId().orElse(null),
						proposal.environmentNameSnapshot(),
						proposal.availableEquipmentSnapshot()),
				proposal.occurrenceVersionAtGeneration(),
				proposal.occurrenceUpdatedAtAtGeneration(),
				proposal.feasibilityFingerprint().value(),
				proposal.status(),
				proposal.totalExecutions(),
				proposal.alreadyFeasibleExecutions(),
				proposal.proposedSubstitutions(),
				proposal.unresolvedExecutions(),
				proposal.excludedExecutions(),
				proposal.expectedFeasibleExecutions(),
				proposal.expectedFeasibilityPercentage(),
				proposal.expectedFeasibilityIfAllProposedAccepted(),
				proposal.acceptedFeasibilityExecutions(),
				proposal.unresolvedCount(),
				proposal.generatedAt(),
				proposal.expiresAt(),
				proposal.appliedAt(),
				proposal.cancelledAt(),
				items,
				proposal.createdAt(),
				proposal.updatedAt(),
				proposal.version());
	}

	static WorkoutAdaptationProposalSummaryResult toSummaryResult(WorkoutAdaptationProposal proposal) {
		return new WorkoutAdaptationProposalSummaryResult(
				proposal.id(),
				proposal.workoutOccurrenceId(),
				proposal.status(),
				proposal.totalExecutions(),
				proposal.expectedFeasibleExecutions(),
				proposal.expectedFeasibilityPercentage(),
				proposal.unresolvedCount(),
				proposal.generatedAt(),
				proposal.expiresAt(),
				proposal.version());
	}

	private static WorkoutAdaptationProposalItemResult toItemResult(WorkoutAdaptationProposalItem item) {
		List<WorkoutAdaptationAlternativeResult> alternatives = item.alternatives().stream()
				.map(WorkoutAdaptationProposalSupport::toAlternativeResult)
				.toList();
		return new WorkoutAdaptationProposalItemResult(
				item.id(),
				item.workoutExerciseExecutionId(),
				item.sourceWorkoutExerciseId(),
				item.executionOrder(),
				item.prescribedExerciseDefinitionId(),
				item.prescribedNameSnapshot(),
				item.currentPerformedExerciseDefinitionId(),
				item.currentPerformedNameSnapshot(),
				item.currentFeasible(),
				item.prescribedFeasible(),
				item.performedFeasible(),
				item.missingRequiredEquipment(),
				item.analysisReasonCode(),
				item.action(),
				item.generatedTargetExerciseDefinitionId(),
				item.generatedTargetNameSnapshot(),
				item.generatedRelationshipId(),
				item.generatedRelationshipTypeSnapshot(),
				item.generatedCompatibilitySnapshot(),
				item.generatedRationaleSnapshot(),
				item.selectedTargetExerciseDefinitionId(),
				item.selectedRelationshipId(),
				item.athleteDecision(),
				item.athleteNotes(),
				alternatives,
				item.createdAt(),
				item.updatedAt(),
				item.version());
	}

	private static WorkoutAdaptationAlternativeResult toAlternativeResult(WorkoutAdaptationAlternative alternative) {
		return new WorkoutAdaptationAlternativeResult(
				alternative.id(),
				alternative.rankPosition(),
				alternative.relationshipId(),
				alternative.targetExerciseDefinitionId(),
				alternative.targetNameSnapshot(),
				alternative.relationshipTypeSnapshot(),
				alternative.compatibilitySnapshot(),
				alternative.rationaleSnapshot(),
				alternative.targetDifficultySnapshot(),
				alternative.targetImpactLevelSnapshot(),
				alternative.requiredEquipment(),
				alternative.selectedDefault());
	}

	static List<WorkoutAdaptationAlternative> toAlternatives(
			List<ExerciseSubstitutionSuggestionResult> suggestions,
			ExerciseDefinitionId defaultTargetId) {
		if (suggestions.isEmpty()) {
			return List.of();
		}
		List<WorkoutAdaptationAlternative> alternatives = new ArrayList<>(suggestions.size());
		for (ExerciseSubstitutionSuggestionResult suggestion : suggestions) {
			alternatives.add(new WorkoutAdaptationAlternative(
					WorkoutAdaptationAlternativeId.generate(),
					suggestion.rankingPosition(),
					suggestion.relationshipId(),
					suggestion.targetExerciseDefinitionId(),
					suggestion.targetCanonicalName(),
					suggestion.relationshipType(),
					suggestion.compatibilityLevel(),
					suggestion.rationale(),
					suggestion.targetDifficulty(),
					null,
					suggestion.targetRequiredEquipment(),
					suggestion.targetExerciseDefinitionId().equals(defaultTargetId)));
		}
		return List.copyOf(alternatives);
	}

	static WorkoutOccurrenceEnvironmentSnapshot resolveEnvironmentSnapshot(WorkoutOccurrence occurrence) {
		return WorkoutOccurrenceEnvironmentSupport.resolveSubstitutionContextSnapshot(occurrence);
	}

	private static void collectTargetFlag(
			ExerciseDefinitionId targetId,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, Boolean> targetFlags) {
		if (targetId == null || targetFlags.containsKey(targetId)) {
			return;
		}
		ExerciseDefinition definition = definitionsById.get(targetId);
		targetFlags.put(targetId, definition != null && definition.active());
	}

	private static void collectRelationshipFlag(
			ExerciseSubstitutionRelationshipId relationshipId,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			AthleteId athleteId,
			Map<ExerciseSubstitutionRelationshipId, Boolean> relationshipFlags) {
		if (relationshipId == null || relationshipFlags.containsKey(relationshipId)) {
			return;
		}
		boolean active = relationshipRepository.findActiveById(relationshipId)
				.filter(relationship -> ExerciseSubstitutionRelationshipAccessPolicy.isAccessible(athleteId, relationship))
				.isPresent();
		relationshipFlags.put(relationshipId, active);
	}

}
