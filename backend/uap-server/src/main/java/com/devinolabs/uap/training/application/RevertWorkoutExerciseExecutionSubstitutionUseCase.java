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
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseNotSubstitutedException;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Puts a substituted execution back on its prescribed movement.
 *
 * <p>The reversion is appended to the log rather than erasing the substitution, so the record still
 * shows that the athlete swapped and then changed their mind.
 */
@Service
public class RevertWorkoutExerciseExecutionSubstitutionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final Clock clock;

	public RevertWorkoutExerciseExecutionSubstitutionUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.substitutionHistoryRepository = Objects.requireNonNull(substitutionHistoryRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseExecutionResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			String notes) {
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
		if (!execution.isSubstituted()) {
			throw new WorkoutExerciseNotSubstitutedException();
		}

		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				execution.id(), athleteId);
		WorkoutExerciseExecutionSupport.requireSubstitutable(execution, sets);

		ExerciseDefinitionId previousDefinitionId = execution.performedExerciseDefinitionId();
		String previousName = execution.performedExerciseNameSnapshot();
		ExerciseDefinition prescribedDefinition = exerciseDefinitionRepository
				.findById(execution.prescribedExerciseDefinitionId())
				.orElseThrow(ExerciseDefinitionNotFoundException::new);
		execution.revertSubstitution(prescribedDefinition, clock);
		WorkoutExerciseExecution reverted = workoutExerciseExecutionRepository.save(execution);
		substitutionHistoryRepository.append(WorkoutExerciseSubstitutionHistory.reversion(
				reverted, previousDefinitionId, previousName, notes, clock));
		return WorkoutExerciseExecutionSupport.toResult(
				reverted, workoutExerciseSetRepository, athleteId);
	}

}
