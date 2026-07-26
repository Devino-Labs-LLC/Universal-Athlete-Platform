package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class ReorderWorkoutExerciseSetsUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	ReorderWorkoutExerciseSetsUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			Clock clock) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public List<WorkoutExerciseSetResult> execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			List<UUID> setIds) {
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForWrite(
				accountId, planId, dayId, occurrenceId, executionId);

		if (setIds == null || setIds.isEmpty()) {
			throw new InvalidWorkoutExerciseSetMembershipException("setIds must not be empty");
		}
		if (setIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidWorkoutExerciseSetMembershipException("setIds must not contain null values");
		}
		Set<UUID> unique = new HashSet<>();
		for (UUID id : setIds) {
			if (!unique.add(id)) {
				throw new InvalidWorkoutExerciseSetMembershipException("setIds must not contain duplicates");
			}
		}

		List<WorkoutExerciseSet> existing = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				context.execution().id(), context.athleteId());
		if (existing.stream().anyMatch(set -> set.status() != WorkoutExerciseSetStatus.NOT_STARTED)) {
			throw new WorkoutExerciseSetReorderNotAllowedException(
					"Sets can only be reordered while every set is NOT_STARTED");
		}
		if (existing.size() != setIds.size()) {
			throw new InvalidWorkoutExerciseSetMembershipException(
					"setIds must include every workout exercise set exactly once");
		}
		Map<UUID, WorkoutExerciseSet> byId = existing.stream()
				.collect(Collectors.toMap(set -> set.id().value(), Function.identity()));
		for (UUID id : setIds) {
			if (!byId.containsKey(id)) {
				throw new InvalidWorkoutExerciseSetMembershipException("setIds contains an unknown workout exercise set");
			}
		}

		WorkoutExerciseSetSupport.resequence(
				setIds.stream().map(byId::get).toList(), workoutExerciseSetRepository, clock);
		return WorkoutExerciseSetSupport.toResults(
				workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
						context.execution().id(), context.athleteId()));
	}

}
