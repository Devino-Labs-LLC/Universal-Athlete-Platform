package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class CreateWorkoutOccurrenceUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;
	private final Clock clock;

	public CreateWorkoutOccurrenceUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			TrainingEnvironmentRepository trainingEnvironmentRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.trainingEnvironmentRepository = Objects.requireNonNull(trainingEnvironmentRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutOccurrenceDetailResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			String athleteNotes) {
		AthleteRef athlete = WorkoutOccurrenceSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);

		List<WorkoutExercise> exercises = workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId);
		if (exercises.isEmpty()) {
			throw new WorkoutOccurrenceRequiresExercisesException();
		}

		WorkoutOccurrenceSupport.assertUniqueActiveDate(
				workoutOccurrenceRepository, day.id(), athleteId, scheduledDate, null);

		WorkoutOccurrenceId occurrenceId = WorkoutOccurrenceId.generate();
		WorkoutOccurrence occurrence = WorkoutOccurrence.createManual(
				occurrenceId,
				plan.id(),
				day.id(),
				athleteId,
				scheduledDate,
				plannedStartTime,
				athleteNotes,
				clock);

		WorkoutOccurrenceEnvironmentSnapshot planned = TrainingEnvironmentSupport.resolvePreferredSnapshot(
				trainingEnvironmentRepository, day, plan, athleteId);
		occurrence.initializeEnvironmentContext(planned, clock);

		List<WorkoutExerciseExecution> executions = new ArrayList<>(exercises.size());
		Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById = exerciseDefinitionRepository
				.findAllByIds(exercises.stream().map(WorkoutExercise::exerciseDefinitionId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(ExerciseDefinition::id, Function.identity()));
		for (WorkoutExercise exercise : exercises) {
			ExerciseDefinition definition = definitionsById.get(exercise.exerciseDefinitionId());
			if (definition == null) {
				throw new ExerciseDefinitionNotFoundException();
			}
			executions.add(WorkoutExerciseExecution.fromPrescription(exercise, definition, occurrenceId, clock));
		}

		WorkoutOccurrence saved = workoutOccurrenceRepository.save(occurrence);
		List<WorkoutExerciseExecution> savedExecutions = workoutExerciseExecutionRepository.saveAll(executions);
		List<WorkoutExerciseSet> sets = new ArrayList<>();
		for (WorkoutExerciseExecution execution : savedExecutions) {
			sets.addAll(WorkoutExerciseSetSupport.createInitialSets(execution, clock));
		}
		workoutExerciseSetRepository.saveAll(sets);
		return WorkoutOccurrenceSupport.toDetailResult(
				saved,
				WorkoutExerciseExecutionSupport.toResults(savedExecutions, workoutExerciseSetRepository, athleteId));
	}

}
