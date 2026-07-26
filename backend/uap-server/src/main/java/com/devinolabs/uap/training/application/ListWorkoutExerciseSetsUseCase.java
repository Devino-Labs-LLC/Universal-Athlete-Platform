package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class ListWorkoutExerciseSetsUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;

	ListWorkoutExerciseSetsUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
	}

	@Transactional(readOnly = true)
	public List<WorkoutExerciseSetResult> execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForRead(
				accountId, planId, dayId, occurrenceId, executionId);
		return WorkoutExerciseSetSupport.toResults(
				workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
						context.execution().id(), context.athleteId()));
	}

}
