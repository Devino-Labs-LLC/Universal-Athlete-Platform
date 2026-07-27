package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;

/**
 * Paged performance history for one exercise, newest session first.
 */
@Service
public class GetAthleteExercisePerformanceHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;

	public GetAthleteExercisePerformanceHistoryUseCase(
			AthleteContextPort athleteContextPort,
			ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exercisePerformanceHistoryRepository = Objects.requireNonNull(exercisePerformanceHistoryRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
	}

	@Transactional(readOnly = true)
	public AthleteExercisePerformanceHistoryResult execute(
			AccountId accountId,
			ExercisePerformanceKey exercisePerformanceKey,
			LocalDate scheduledFrom,
			LocalDate scheduledTo,
			Integer page,
			Integer size) {
		Objects.requireNonNull(exercisePerformanceKey, "exercisePerformanceKey must not be null");
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		if (!exercisePerformanceHistoryRepository.existsByAthleteIdAndExercisePerformanceKey(
				athleteId, exercisePerformanceKey)) {
			throw new ExercisePerformanceKeyNotFoundException();
		}
		TrainingPerformanceSupport.requireScheduledRange(scheduledFrom, scheduledTo);
		int resolvedPage = TrainingPerformanceSupport.requirePage(page);
		int resolvedSize = TrainingPerformanceSupport.requireSize(size);

		ExercisePerformanceExecutionPage executions = exercisePerformanceHistoryRepository
				.findCompletedExecutions(
						athleteId, exercisePerformanceKey, scheduledFrom, scheduledTo, resolvedPage, resolvedSize);
		List<WorkoutExerciseExecutionId> executionIds = executions.rows().stream()
				.map(row -> row.execution().id())
				.toList();
		Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> sets = TrainingPerformanceSupport
				.setsByExecution(workoutExerciseSetRepository, executionIds, athleteId);

		List<ExerciseExecutionPerformanceResult> entries = executions.rows().stream()
				.map(row -> TrainingPerformanceSupport.toExecutionResult(
						row, sets.getOrDefault(row.execution().id(), List.of())))
				.toList();
		return new AthleteExercisePerformanceHistoryResult(
				exercisePerformanceKey,
				entries.isEmpty() ? null : entries.getFirst().exerciseName(),
				entries,
				executions.page(),
				executions.size(),
				executions.totalElements(),
				executions.totalPages());
	}

}
