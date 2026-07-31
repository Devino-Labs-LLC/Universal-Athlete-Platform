package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingLoadNumericOverflowException;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutLoadCalculator;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummaryId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;

@Component
public class WorkoutLoadCalculationSupport {

	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final Clock clock;

	public WorkoutLoadCalculationSupport(
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			Clock clock) {
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.loadSummaryRepository = Objects.requireNonNull(loadSummaryRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	public WorkoutOccurrenceLoadSummary calculateAndPersist(
			WorkoutOccurrence occurrence,
			AthleteId athleteId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutSessionEffort sessionEffort,
			Instant sourceUpdatedAt) {
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		try {
			List<WorkoutExerciseExecution> executions = workoutExerciseExecutionRepository
					.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
			Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> sets =
					TrainingPerformanceSupport.setsByExecution(
							workoutExerciseSetRepository,
							executions.stream().map(WorkoutExerciseExecution::id).toList(),
							athleteId);
			WorkoutSessionEffort effort = sessionEffort;
			if (effort == null) {
				effort = sessionEffortRepository
						.findByOccurrenceIdAndAthleteId(occurrence.id(), athleteId)
						.orElse(null);
			}
			Instant calculatedAt = Instant.now(clock);
			WorkoutLoadCalculator.Input input = new WorkoutLoadCalculator.Input(
					athleteId,
					planId,
					dayId,
					occurrence.id(),
					occurrence.scheduledDate(),
					executions,
					sets,
					effort,
					occurrence,
					sourceUpdatedAt);
			WorkoutLoadCalculator.Result result = WorkoutLoadCalculator.calculate(input);
			Optional<WorkoutOccurrenceLoadSummary> existing = loadSummaryRepository
					.findByOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
			WorkoutOccurrenceLoadSummary summary = WorkoutLoadCalculator.toSummary(
					existing.map(WorkoutOccurrenceLoadSummary::id).orElse(WorkoutOccurrenceLoadSummaryId.generate()),
					input,
					result,
					calculatedAt,
					clock,
					existing.map(WorkoutOccurrenceLoadSummary::version).orElse(0L),
					existing.isEmpty());
			return loadSummaryRepository.save(summary);
		}
		catch (TrainingLoadNumericOverflowException ex) {
			throw new WorkoutLoadCalculationFailedException(ex);
		}
		catch (RuntimeException ex) {
			if (ex instanceof WorkoutLoadCalculationFailedException) {
				throw ex;
			}
			throw new WorkoutLoadCalculationFailedException(ex);
		}
	}

}
