package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class DeleteWorkoutExerciseSetUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	DeleteWorkoutExerciseSetUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			Clock clock) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			WorkoutExerciseSetId setId) {
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForWrite(
				accountId, planId, dayId, occurrenceId, executionId);
		WorkoutExerciseSet set = WorkoutExerciseSetSupport.requireOwnedSet(
				workoutExerciseSetRepository, setId, context.execution().id(), context.athleteId());
		if (set.status() != WorkoutExerciseSetStatus.NOT_STARTED) {
			throw new WorkoutExerciseSetDeleteNotAllowedException(
					"Only NOT_STARTED workout exercise sets can be deleted");
		}

		List<WorkoutExerciseSet> existing = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				context.execution().id(), context.athleteId());
		if (existing.size() <= 1) {
			throw new WorkoutExerciseExecutionRequiresSetException();
		}

		workoutExerciseSetRepository.delete(set);

		List<WorkoutExerciseSet> remaining = new ArrayList<>(existing.size() - 1);
		for (WorkoutExerciseSet candidate : existing) {
			if (!candidate.id().equals(set.id())) {
				remaining.add(candidate);
			}
		}
		WorkoutExerciseSetSupport.resequence(remaining, workoutExerciseSetRepository, clock);
	}

}
