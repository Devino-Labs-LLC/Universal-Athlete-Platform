package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetricCalculator;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetrics;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Re-derives one completed execution's metrics and re-applies its personal record candidates.
 *
 * <p>Records are monotonic bests, so replaying an execution that has already been processed leaves
 * the projection untouched: every candidate ties its own standing record and ties change nothing.
 */
@Service
public class RecomputeWorkoutExerciseExecutionMetricsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final ExerciseMetricProcessor exerciseMetricProcessor;
	private final WorkoutLoadCalculationSupport loadCalculationSupport;

	public RecomputeWorkoutExerciseExecutionMetricsUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			ExerciseMetricProcessor exerciseMetricProcessor,
			WorkoutLoadCalculationSupport loadCalculationSupport) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.exerciseMetricProcessor = Objects.requireNonNull(exerciseMetricProcessor);
		this.loadCalculationSupport = Objects.requireNonNull(loadCalculationSupport);
	}

	@Transactional
	public ExerciseExecutionPerformanceResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		AthleteRef athlete = TrainingPerformanceSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		WorkoutExerciseExecution execution = WorkoutExerciseExecutionSupport.requireOwnedExecution(
				workoutExerciseExecutionRepository, executionId, occurrenceId, day.id(), athleteId);
		if (execution.status() != WorkoutExerciseExecutionStatus.COMPLETED) {
			throw new TrainingMetricsRequireCompletedExecutionException();
		}

		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				execution.id(), athleteId);
		if (ExercisePerformanceMetricCalculator.eligibleSets(sets).isEmpty()) {
			throw new TrainingMetricsRequireCompletedSetsException();
		}

		try {
			ExercisePerformanceMetrics metrics = exerciseMetricProcessor.process(
					athleteId, execution, occurrence.status(), occurrence.scheduledDate(), sets);
			if (occurrence.status() == WorkoutOccurrenceStatus.COMPLETED) {
				loadCalculationSupport.calculateAndPersist(
						occurrence, athleteId, plan.id(), day.id(), null, occurrence.updatedAt());
			}
			return new ExerciseExecutionPerformanceResult(
					execution.id(),
					execution.workoutOccurrenceId(),
					execution.exercisePerformanceKey(),
					execution.exerciseName(),
					execution.category(),
					execution.type(),
					execution.displayOrder(),
					execution.status(),
					occurrence.scheduledDate(),
					execution.completedAt(),
					metrics);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new TrainingMetricsRecomputationConflictException(ex);
		}
	}

}
