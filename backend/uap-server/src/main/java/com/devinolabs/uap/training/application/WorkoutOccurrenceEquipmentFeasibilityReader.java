package com.devinolabs.uap.training.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatusResolver;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;

/**
 * Bounded equipment-gap feasibility read for client facades.
 *
 * <p>Loads executions and required-equipment facts only — no substitution relationship trees,
 * ranked suggestions, or full exercise-definition aggregate hydration.
 */
@Component
class WorkoutOccurrenceEquipmentFeasibilityReader {

	private final WorkoutExerciseExecutionRepository executionRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;

	WorkoutOccurrenceEquipmentFeasibilityReader(
			WorkoutExerciseExecutionRepository executionRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository) {
		this.executionRepository = Objects.requireNonNull(executionRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
	}

	WorkoutFeasibilityStatus statusFor(
			AthleteId athleteId,
			WorkoutOccurrence occurrence) {
		Summary summary = summarize(athleteId, occurrence, null);
		return summary == null ? null : summary.status();
	}

	Summary summarize(
			AthleteId athleteId,
			WorkoutOccurrence occurrence,
			List<WorkoutExerciseExecution> preloadedExecutions) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		FeasibilityEnvironmentContextResult environmentContext =
				WorkoutFeasibilitySupport.resolveOccurrenceEnvironment(occurrence);
		if (environmentContext == null) {
			return null;
		}
		List<WorkoutExerciseExecution> executions = preloadedExecutions != null
				? preloadedExecutions
				: executionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
		if (executions.isEmpty()) {
			var empty = WorkoutFeasibilityStatusResolver.summarize(0, 0, 0, 0, 0, true);
			return Summary.from(empty);
		}

		Set<ExerciseDefinitionId> definitionIds = new LinkedHashSet<>();
		for (WorkoutExerciseExecution execution : executions) {
			definitionIds.add(execution.performedExerciseDefinitionId());
		}
		Map<ExerciseDefinitionId, Set<EquipmentType>> requiredByDefinition =
				exerciseDefinitionRepository.findAccessibleActiveRequiredEquipmentByIds(
						List.copyOf(definitionIds), athleteId);

		int feasible = 0;
		for (WorkoutExerciseExecution execution : executions) {
			Set<EquipmentType> required = requiredByDefinition.get(execution.performedExerciseDefinitionId());
			if (required == null) {
				continue;
			}
			if (ExerciseEnvironmentCompatibilityEvaluator.evaluate(
					required, environmentContext.availableEquipment()).compatible()) {
				feasible++;
			}
		}
		var summary = WorkoutFeasibilityStatusResolver.summarize(
				executions.size(),
				feasible,
				0,
				0,
				0,
				true);
		return Summary.from(summary);
	}

	record Summary(
			WorkoutFeasibilityStatus status,
			int totalExercises,
			int feasibleExercises,
			int infeasibleExercises,
			java.math.BigDecimal feasibilityPercentage) {

		static Summary from(WorkoutFeasibilityStatusResolver.WorkoutFeasibilitySummary summary) {
			return new Summary(
					summary.status(),
					summary.totalExercises(),
					summary.feasibleExercises(),
					summary.infeasibleExercises(),
					summary.feasibilityPercentage());
		}
	}

}
