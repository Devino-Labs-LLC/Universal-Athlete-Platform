package com.devinolabs.uap.training.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatusResolver;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
class WorkoutFeasibilityAnalyzer {

	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;

	WorkoutFeasibilityAnalyzer(
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository) {
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
	}

	WorkoutDayFeasibilityResult analyzeDay(
			TrainingPlan plan,
			WorkoutDay day,
			AthleteId athleteId,
			FeasibilityEnvironmentContextResult environmentContext,
			List<WorkoutExercise> exercises,
			int suggestionLimit,
			boolean includeAlternatives) {
		AnalysisContext context = buildPrescriptionContext(athleteId, exercises);
		List<ExerciseFeasibilityAnalysisResult> analyzed = new ArrayList<>();
		int feasibleExercises = 0;
		int exercisesWithSuggestions = 0;
		for (WorkoutExercise exercise : exercises) {
			WorkoutFeasibilitySupport.PrescriptionAnalysis analysis = WorkoutFeasibilitySupport.analyzePrescription(
					athleteId,
					exercise.exerciseDefinitionId(),
					exercise.exerciseName(),
					environmentContext,
					context.definitionsById(),
					context.relationshipsBySource(),
					suggestionLimit,
					includeAlternatives);
			if (analysis.feasible()) {
				feasibleExercises++;
			}
			if (analysis.hasCompatibleSubstitution()) {
				exercisesWithSuggestions++;
			}
			analyzed.add(new ExerciseFeasibilityAnalysisResult(
					exercise.id(),
					exercise.exerciseDefinitionId(),
					exercise.exerciseName(),
					exercise.displayOrder(),
					analysis.feasible(),
					analysis.compatibility(),
					analysis.currentStatus(),
					analysis.reasonCode(),
					analysis.reasonSummary(),
					analysis.compatibleSubstitutionCount(),
					analysis.suggestedSubstitutions(),
					analysis.hasCompatibleSubstitution()));
		}
		var summary = WorkoutFeasibilityStatusResolver.summarize(
				exercises.size(),
				feasibleExercises,
				0,
				0,
				exercisesWithSuggestions,
				environmentContext != null);
		return new WorkoutDayFeasibilityResult(
				plan.id(),
				day.id(),
				day.title(),
				environmentContext,
				WorkoutFeasibilitySummaryResult.from(summary),
				analyzed);
	}

	WorkoutOccurrenceFeasibilityResult analyzeOccurrence(
			TrainingPlan plan,
			WorkoutDay day,
			WorkoutOccurrence occurrence,
			AthleteId athleteId,
			FeasibilityEnvironmentContextResult environmentContext,
			List<WorkoutExerciseExecution> executions,
			int suggestionLimit,
			boolean includeAlternatives) {
		AnalysisContext context = buildExecutionContext(athleteId, executions);
		List<WorkoutOccurrenceFeasibilityResult.ExerciseExecutionFeasibilityAnalysisResult> analyzed =
				new ArrayList<>();
		int feasibleExecutions = 0;
		int substitutedExecutions = 0;
		int feasibleAfterSubstitution = 0;
		int exercisesWithSuggestions = 0;
		for (WorkoutExerciseExecution execution : executions) {
			boolean substituted = execution.isSubstituted();
			if (substituted) {
				substitutedExecutions++;
			}
			WorkoutFeasibilitySupport.ExecutionAnalysis analysis = WorkoutFeasibilitySupport.analyzeExecution(
					athleteId,
					execution.prescribedExerciseDefinitionId(),
					execution.prescribedExerciseNameSnapshot(),
					execution.performedExerciseDefinitionId(),
					execution.performedExerciseNameSnapshot(),
					substituted,
					environmentContext,
					context.definitionsById(),
					context.relationshipsBySource(),
					suggestionLimit,
					includeAlternatives);
			if (analysis.currentExecutionFeasible()) {
				feasibleExecutions++;
				if (substituted) {
					feasibleAfterSubstitution++;
				}
			}
			if (analysis.hasCompatibleSubstitution()) {
				exercisesWithSuggestions++;
			}
			analyzed.add(new WorkoutOccurrenceFeasibilityResult.ExerciseExecutionFeasibilityAnalysisResult(
					execution.id(),
					execution.displayOrder(),
					execution.prescribedExerciseDefinitionId(),
					execution.prescribedExerciseNameSnapshot(),
					execution.performedExerciseDefinitionId(),
					execution.performedExerciseNameSnapshot(),
					substituted,
					analysis.prescribedCompatibility(),
					analysis.performedCompatibility(),
					analysis.currentExecutionFeasible(),
					analysis.currentStatus(),
					analysis.reasonCode(),
					analysis.reasonSummary(),
					analysis.compatibleSubstitutionCount(),
					analysis.suggestedSubstitutions(),
					analysis.hasCompatibleSubstitution()));
		}
		var summary = WorkoutFeasibilityStatusResolver.summarize(
				executions.size(),
				feasibleExecutions,
				substitutedExecutions,
				feasibleAfterSubstitution,
				exercisesWithSuggestions,
				environmentContext != null);
		return new WorkoutOccurrenceFeasibilityResult(
				plan.id(),
				day.id(),
				occurrence.id(),
				environmentContext,
				WorkoutFeasibilitySummaryResult.from(summary),
				analyzed);
	}

	private AnalysisContext buildPrescriptionContext(AthleteId athleteId, List<WorkoutExercise> exercises) {
		List<ExerciseDefinitionId> sourceIds = exercises.stream()
				.map(WorkoutExercise::exerciseDefinitionId)
				.distinct()
				.toList();
		Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource =
				WorkoutFeasibilitySupport.relationshipsBySource(
						relationshipRepository, sourceIds, athleteId);
		Set<ExerciseDefinitionId> definitionIds = new LinkedHashSet<>(sourceIds);
		for (List<ExerciseSubstitutionRelationship> relationships : relationshipsBySource.values()) {
			for (ExerciseSubstitutionRelationship relationship : relationships) {
				definitionIds.add(relationship.targetExerciseDefinitionId());
			}
		}
		Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById =
				WorkoutFeasibilitySupport.loadDefinitions(exerciseDefinitionRepository, List.copyOf(definitionIds));
		return new AnalysisContext(definitionsById, relationshipsBySource);
	}

	private AnalysisContext buildExecutionContext(AthleteId athleteId, List<WorkoutExerciseExecution> executions) {
		Set<ExerciseDefinitionId> sourceIds = new LinkedHashSet<>();
		for (WorkoutExerciseExecution execution : executions) {
			sourceIds.add(execution.prescribedExerciseDefinitionId());
		}
		Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource =
				WorkoutFeasibilitySupport.relationshipsBySource(
						relationshipRepository, List.copyOf(sourceIds), athleteId);
		Set<ExerciseDefinitionId> definitionIds = new LinkedHashSet<>(sourceIds);
		for (WorkoutExerciseExecution execution : executions) {
			definitionIds.add(execution.performedExerciseDefinitionId());
		}
		for (List<ExerciseSubstitutionRelationship> relationships : relationshipsBySource.values()) {
			for (ExerciseSubstitutionRelationship relationship : relationships) {
				definitionIds.add(relationship.targetExerciseDefinitionId());
			}
		}
		Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById =
				WorkoutFeasibilitySupport.loadDefinitions(exerciseDefinitionRepository, List.copyOf(definitionIds));
		return new AnalysisContext(definitionsById, relationshipsBySource);
	}

	private record AnalysisContext(
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource) {
	}

}
