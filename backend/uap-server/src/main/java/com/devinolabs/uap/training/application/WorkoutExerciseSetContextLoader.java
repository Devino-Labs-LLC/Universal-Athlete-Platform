package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Resolves the plan/day/occurrence/execution chain a set operation hangs off, and owns the parent
 * write guard: sets are writable only while the occurrence is SCHEDULED or IN_PROGRESS and the
 * execution is NOT_STARTED or IN_PROGRESS.
 */
@Component
class WorkoutExerciseSetContextLoader {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final Clock clock;

	WorkoutExerciseSetContextLoader(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	SetContext loadForRead(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		return load(athleteId, plan, dayId, occurrenceId, executionId);
	}

	SetContext loadForWrite(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requireMutablePlan(
				trainingPlanRepository, athleteId, planId);
		SetContext context = load(athleteId, plan, dayId, occurrenceId, executionId);
		requireSetWritable(context.occurrence(), context.execution());
		return context;
	}

	/**
	 * Loads a writable context and promotes the parents: a SCHEDULED occurrence becomes IN_PROGRESS
	 * and a NOT_STARTED execution becomes IN_PROGRESS before the set mutation is applied.
	 */
	SetContext loadForWriteAndPromoteParents(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		SetContext context = loadForWrite(accountId, planId, dayId, occurrenceId, executionId);
		WorkoutOccurrence occurrence = context.occurrence();
		if (occurrence.status() == WorkoutOccurrenceStatus.SCHEDULED) {
			try {
				occurrence.start(clock);
			}
			catch (IllegalStateException ex) {
				throw WorkoutOccurrenceSupport.translateStatus(ex);
			}
			occurrence = workoutOccurrenceRepository.save(occurrence);
		}
		WorkoutExerciseExecution execution = context.execution();
		if (execution.status() == WorkoutExerciseExecutionStatus.NOT_STARTED) {
			try {
				execution.start(clock);
			}
			catch (IllegalStateException ex) {
				throw WorkoutExerciseExecutionSupport.translateStatus(ex);
			}
			execution = workoutExerciseExecutionRepository.save(execution);
		}
		return new SetContext(context.athleteId(), occurrence, execution);
	}

	private SetContext load(
			AthleteId athleteId,
			TrainingPlan plan,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		WorkoutExerciseExecution execution = WorkoutExerciseExecutionSupport.requireOwnedExecution(
				workoutExerciseExecutionRepository, executionId, occurrenceId, day.id(), athleteId);
		return new SetContext(athleteId, occurrence, execution);
	}

	private static void requireSetWritable(WorkoutOccurrence occurrence, WorkoutExerciseExecution execution) {
		WorkoutExerciseExecutionSupport.requireExecutionWritable(occurrence);
		if (execution.status() != WorkoutExerciseExecutionStatus.NOT_STARTED
				&& execution.status() != WorkoutExerciseExecutionStatus.IN_PROGRESS) {
			throw new InvalidWorkoutExerciseExecutionStatusException(
					"Workout exercise sets cannot be modified when the execution is " + execution.status());
		}
	}

	record SetContext(AthleteId athleteId, WorkoutOccurrence occurrence, WorkoutExerciseExecution execution) {
	}

}
