package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class StartWorkoutExerciseSetUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	StartWorkoutExerciseSetUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			Clock clock) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseSetResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			WorkoutExerciseSetId setId) {
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForWriteAndPromoteParents(
				accountId, planId, dayId, occurrenceId, executionId);
		WorkoutExerciseSet set = WorkoutExerciseSetSupport.requireOwnedSet(
				workoutExerciseSetRepository, setId, context.execution().id(), context.athleteId());
		try {
			set.start(clock);
		}
		catch (IllegalStateException ex) {
			throw WorkoutExerciseSetSupport.translateStatus(ex);
		}
		return WorkoutExerciseSetSupport.toResult(workoutExerciseSetRepository.save(set));
	}

}
