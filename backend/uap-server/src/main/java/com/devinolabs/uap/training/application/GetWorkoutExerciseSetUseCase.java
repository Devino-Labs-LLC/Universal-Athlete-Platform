package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class GetWorkoutExerciseSetUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;

	GetWorkoutExerciseSetUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutExerciseSetResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			WorkoutExerciseSetId setId) {
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForRead(
				accountId, planId, dayId, occurrenceId, executionId);
		return WorkoutExerciseSetSupport.toResult(WorkoutExerciseSetSupport.requireOwnedSet(
				workoutExerciseSetRepository, setId, context.execution().id(), context.athleteId()));
	}

}
