package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Swaps the movement an athlete performs for one occurrence without touching the plan.
 *
 * <p>The prescription row and every prescribed snapshot on the execution are left alone, so the
 * next occurrence generated from the same plan is prescribed the original movement again. Set
 * prescriptions are kept as well: the athlete chose a stand-in for the same work, not a new one.
 */
@Service
public class SubstituteWorkoutExerciseExecutionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public SubstituteWorkoutExerciseExecutionUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.substitutionHistoryRepository = Objects.requireNonNull(substitutionHistoryRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseExecutionResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionReason reason,
			String notes,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId) {
		Objects.requireNonNull(targetExerciseDefinitionId, "targetExerciseDefinitionId must not be null");
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requireMutablePlan(
				trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		WorkoutExerciseExecutionSupport.requireExecutionWritable(occurrence);
		WorkoutExerciseExecution execution = WorkoutExerciseExecutionSupport.requireOwnedExecution(
				workoutExerciseExecutionRepository, executionId, occurrenceId, day.id(), athleteId);

		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				execution.id(), athleteId);
		WorkoutExerciseExecutionSupport.requireSubstitutable(execution, sets);

		ExerciseDefinition target = ExerciseDefinitionAccessPolicy.requireSelectable(
				athleteId,
				ExerciseDefinitionSupport.requireAccessible(
						exerciseDefinitionRepository, athleteId, targetExerciseDefinitionId));

		ExerciseSubstitutionRelationship relationship = resolveRelationship(
				athleteId, execution, target.id(), substitutionRelationshipId);

		ExerciseDefinitionId previousDefinitionId = execution.performedExerciseDefinitionId();
		String previousName = execution.performedExerciseNameSnapshot();
		execution.substitute(target, reason, notes, clock);
		WorkoutExerciseExecution substituted = workoutExerciseExecutionRepository.save(execution);
		substitutionHistoryRepository.append(WorkoutExerciseSubstitutionHistory.substitution(
				substituted,
				previousDefinitionId,
				previousName,
				reason,
				notes,
				relationship,
				WorkoutOccurrenceEnvironmentSupport.resolveSubstitutionContextSnapshot(occurrence),
				clock));
		return WorkoutExerciseExecutionSupport.toResult(
				substituted, workoutExerciseSetRepository, athleteId);
	}

	private ExerciseSubstitutionRelationship resolveRelationship(
			AthleteId athleteId,
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId) {
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
			throw new ExerciseSubstitutionRelationshipMismatchException(
					"Substitution relationship source must match the currently performed exercise");
		}
		if (!relationship.targetExerciseDefinitionId().equals(targetExerciseDefinitionId)) {
			throw new ExerciseSubstitutionRelationshipMismatchException(
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
