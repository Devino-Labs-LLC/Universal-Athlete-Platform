package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternative;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;

final class WorkoutAdaptationProposalGenerator {

	private WorkoutAdaptationProposalGenerator() {
	}

	static List<WorkoutAdaptationProposalItem> generateItems(
			WorkoutAdaptationProposalId proposalId,
			AthleteId athleteId,
			List<WorkoutExerciseExecution> executions,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			int suggestionLimit,
			boolean includeAlternatives,
			Clock clock) {
		return generateItems(
				proposalId,
				athleteId,
				executions,
				environmentContext,
				definitionsById,
				relationshipsBySource,
				suggestionLimit,
				includeAlternatives,
				clock,
				false);
	}

	static List<WorkoutAdaptationProposalItem> generateItems(
			WorkoutAdaptationProposalId proposalId,
			AthleteId athleteId,
			List<WorkoutExerciseExecution> executions,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			int suggestionLimit,
			boolean includeAlternatives,
			Clock clock,
			boolean preferLowerImpactVariations) {
		List<WorkoutExerciseExecution> ordered = executions.stream()
				.sorted(Comparator.comparingInt(WorkoutExerciseExecution::displayOrder)
						.thenComparing(execution -> execution.id().value()))
				.toList();
		List<WorkoutAdaptationProposalItem> items = new ArrayList<>(ordered.size());
		for (WorkoutExerciseExecution execution : ordered) {
			items.add(generateItem(
					proposalId,
					athleteId,
					execution,
					environmentContext,
					definitionsById,
					relationshipsBySource,
					suggestionLimit,
					includeAlternatives,
					clock,
					preferLowerImpactVariations));
		}
		return List.copyOf(items);
	}

	private static WorkoutAdaptationProposalItem generateItem(
			WorkoutAdaptationProposalId proposalId,
			AthleteId athleteId,
			WorkoutExerciseExecution execution,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			int suggestionLimit,
			boolean includeAlternatives,
			Clock clock,
			boolean preferLowerImpactVariations) {
		boolean substituted = execution.isSubstituted();
		WorkoutFeasibilitySupport.ExecutionAnalysis analysis = WorkoutFeasibilitySupport.analyzeExecution(
				athleteId,
				execution.prescribedExerciseDefinitionId(),
				execution.prescribedExerciseNameSnapshot(),
				execution.performedExerciseDefinitionId(),
				execution.performedExerciseNameSnapshot(),
				substituted,
				environmentContext,
				definitionsById,
				relationshipsBySource,
				suggestionLimit,
				includeAlternatives,
				preferLowerImpactVariations);
		List<com.devinolabs.uap.training.domain.EquipmentType> missingEquipment =
				analysis.performedCompatibility().missingRequiredEquipment();
		if (analysis.currentExecutionFeasible()) {
			return WorkoutAdaptationProposalItem.forGeneration(
					proposalId,
					execution,
					true,
					analysis.prescribedCompatibility().compatible(),
					analysis.performedCompatibility().compatible(),
					missingEquipment,
					analysis.reasonCode(),
					WorkoutAdaptationAction.NO_CHANGE,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(),
					clock);
		}
		List<ExerciseSubstitutionSuggestionResult> suggestions = analysis.suggestedSubstitutions();
		if (suggestions.isEmpty()) {
			return WorkoutAdaptationProposalItem.forGeneration(
					proposalId,
					execution,
					false,
					analysis.prescribedCompatibility().compatible(),
					analysis.performedCompatibility().compatible(),
					missingEquipment,
					analysis.reasonCode(),
					WorkoutAdaptationAction.UNRESOLVED,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(),
					clock);
		}
		ExerciseSubstitutionSuggestionResult defaultSuggestion = suggestions.getFirst();
		List<WorkoutAdaptationAlternative> alternatives = includeAlternatives
				? WorkoutAdaptationProposalSupport.toAlternatives(
						suggestions, defaultSuggestion.targetExerciseDefinitionId())
				: List.of();
		return WorkoutAdaptationProposalItem.forGeneration(
				proposalId,
				execution,
				false,
				analysis.prescribedCompatibility().compatible(),
				analysis.performedCompatibility().compatible(),
				missingEquipment,
				analysis.reasonCode(),
				WorkoutAdaptationAction.SUBSTITUTE,
				defaultSuggestion.targetExerciseDefinitionId(),
				defaultSuggestion.targetCanonicalName(),
				defaultSuggestion.relationshipId(),
				defaultSuggestion.relationshipType(),
				defaultSuggestion.compatibilityLevel(),
				defaultSuggestion.rationale(),
				alternatives,
				clock);
	}

	static Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsForExecutions(
			ExerciseSubstitutionRelationshipRepository repository,
			List<WorkoutExerciseExecution> executions,
			AthleteId athleteId) {
		List<ExerciseDefinitionId> sourceIds = executions.stream()
				.map(WorkoutExerciseExecution::performedExerciseDefinitionId)
				.distinct()
				.collect(Collectors.toList());
		return WorkoutFeasibilitySupport.relationshipsBySource(repository, sourceIds, athleteId);
	}

	static Map<ExerciseDefinitionId, ExerciseDefinition> definitionsForExecutions(
			ExerciseDefinitionRepository repository,
			List<WorkoutExerciseExecution> executions,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource) {
		java.util.Set<ExerciseDefinitionId> definitionIds = new java.util.LinkedHashSet<>();
		for (WorkoutExerciseExecution execution : executions) {
			definitionIds.add(execution.prescribedExerciseDefinitionId());
			definitionIds.add(execution.performedExerciseDefinitionId());
		}
		for (List<ExerciseSubstitutionRelationship> relationships : relationshipsBySource.values()) {
			for (ExerciseSubstitutionRelationship relationship : relationships) {
				definitionIds.add(relationship.targetExerciseDefinitionId());
			}
		}
		return WorkoutFeasibilitySupport.loadDefinitions(repository, List.copyOf(definitionIds));
	}

	static Map<ExerciseDefinitionId, ExerciseDefinition> definitionsForExecutions(
			ExerciseDefinitionRepository repository,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutAdaptationProposalItem> items) {
		List<ExerciseDefinitionId> definitionIds = new ArrayList<>();
		for (WorkoutExerciseExecution execution : executions) {
			definitionIds.add(execution.prescribedExerciseDefinitionId());
			definitionIds.add(execution.performedExerciseDefinitionId());
		}
		for (WorkoutAdaptationProposalItem item : items) {
			if (item.generatedTargetExerciseDefinitionId() != null) {
				definitionIds.add(item.generatedTargetExerciseDefinitionId());
			}
			if (item.selectedTargetExerciseDefinitionId() != null) {
				definitionIds.add(item.selectedTargetExerciseDefinitionId());
			}
			item.alternatives().forEach(alternative -> definitionIds.add(alternative.targetExerciseDefinitionId()));
		}
		return WorkoutFeasibilitySupport.loadDefinitions(repository, definitionIds.stream().distinct().toList());
	}

}
