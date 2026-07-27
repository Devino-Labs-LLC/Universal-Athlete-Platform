package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

final class WorkoutExerciseExecutionSupport {

	private WorkoutExerciseExecutionSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return WorkoutOccurrenceSupport.requireMutableAthlete(athleteContextPort, accountId);
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return WorkoutOccurrenceSupport.requireAthlete(athleteContextPort, accountId);
	}

	static TrainingPlan requireMutablePlan(
			TrainingPlanRepository trainingPlanRepository,
			AthleteId athleteId,
			TrainingPlanId planId) {
		return WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
	}

	static TrainingPlan requirePlan(
			TrainingPlanRepository trainingPlanRepository,
			AthleteId athleteId,
			TrainingPlanId planId) {
		return WorkoutOccurrenceSupport.requirePlan(trainingPlanRepository, athleteId, planId);
	}

	static WorkoutDay requireOwnedDay(
			WorkoutDayRepository workoutDayRepository,
			TrainingPlanId planId,
			AthleteId athleteId,
			WorkoutDayId dayId) {
		return WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, planId, athleteId, dayId);
	}

	static WorkoutOccurrence requireOwnedOccurrence(
			WorkoutOccurrenceRepository repository,
			WorkoutOccurrenceId occurrenceId,
			WorkoutDayId dayId,
			AthleteId athleteId) {
		return WorkoutOccurrenceSupport.requireOwnedOccurrence(repository, occurrenceId, dayId, athleteId);
	}

	/**
	 * Child execution mutations are allowed only while the parent is SCHEDULED or IN_PROGRESS.
	 * Starting a child from SCHEDULED atomically promotes the parent to IN_PROGRESS.
	 */
	static void requireExecutionWritable(WorkoutOccurrence occurrence) {
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		if (occurrence.status() == WorkoutOccurrenceStatus.COMPLETED
				|| occurrence.status() == WorkoutOccurrenceStatus.SKIPPED
				|| occurrence.status() == WorkoutOccurrenceStatus.CANCELLED) {
			throw new InvalidWorkoutOccurrenceStatusException(
					"Workout exercise executions cannot be modified when the occurrence is "
							+ occurrence.status());
		}
	}

	static WorkoutOccurrence ensureOccurrenceInProgress(
			WorkoutOccurrence occurrence,
			WorkoutOccurrenceRepository occurrenceRepository,
			Clock clock) {
		requireExecutionWritable(occurrence);
		if (occurrence.status() == WorkoutOccurrenceStatus.SCHEDULED) {
			try {
				occurrence.start(clock);
			}
			catch (IllegalStateException ex) {
				throw WorkoutOccurrenceSupport.translateStatus(ex);
			}
			return occurrenceRepository.save(occurrence);
		}
		return occurrence;
	}

	static WorkoutExerciseExecution requireOwnedExecution(
			WorkoutExerciseExecutionRepository repository,
			WorkoutExerciseExecutionId executionId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutDayId dayId,
			AthleteId athleteId) {
		return repository
				.findByIdAndWorkoutDayIdAndAthleteId(executionId, dayId, occurrenceId, athleteId)
				.orElseThrow(WorkoutExerciseExecutionNotFoundException::new);
	}

	static WorkoutExerciseExecutionResult toResult(
			WorkoutExerciseExecution execution,
			WorkoutExerciseSetRepository setRepository,
			AthleteId athleteId) {
		return toResult(execution, WorkoutExerciseSetSupport
				.countsByExecution(setRepository, List.of(execution.id()), athleteId)
				.getOrDefault(execution.id(), WorkoutExerciseSetCounts.none()));
	}

	static List<WorkoutExerciseExecutionResult> toResults(
			List<WorkoutExerciseExecution> executions,
			WorkoutExerciseSetRepository setRepository,
			AthleteId athleteId) {
		Map<WorkoutExerciseExecutionId, WorkoutExerciseSetCounts> counts = WorkoutExerciseSetSupport
				.countsByExecution(setRepository, executions.stream().map(WorkoutExerciseExecution::id).toList(),
						athleteId);
		return executions.stream()
				.map(execution -> toResult(
						execution, counts.getOrDefault(execution.id(), WorkoutExerciseSetCounts.none())))
				.toList();
	}

	static WorkoutExerciseExecutionResult toResult(
			WorkoutExerciseExecution execution,
			WorkoutExerciseSetCounts counts) {
		return new WorkoutExerciseExecutionResult(
				execution.id(),
				execution.sourceWorkoutExerciseId(),
				execution.exerciseDefinitionId(),
				execution.exercisePerformanceKey(),
				execution.displayOrder(),
				execution.exerciseName(),
				execution.category(),
				execution.type(),
				execution.prescribedSets(),
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRestSeconds(),
				execution.prescribedTargetRpe(),
				execution.prescribedTempo(),
				execution.prescribedCoachingNotes(),
				execution.status(),
				execution.actualSets(),
				execution.actualReps(),
				execution.actualWeight(),
				execution.weightUnit(),
				execution.actualDurationSeconds(),
				execution.actualDistance(),
				execution.distanceUnit(),
				execution.actualRestSeconds(),
				execution.actualRpe(),
				execution.startedAt(),
				execution.completedAt(),
				execution.athleteNotes(),
				execution.createdAt(),
				execution.updatedAt(),
				counts);
	}

	static RuntimeException translateStatus(IllegalStateException ex) {
		return new InvalidWorkoutExerciseExecutionStatusException(
				Objects.requireNonNullElse(ex.getMessage(), "Invalid workout exercise execution status transition"));
	}

}
