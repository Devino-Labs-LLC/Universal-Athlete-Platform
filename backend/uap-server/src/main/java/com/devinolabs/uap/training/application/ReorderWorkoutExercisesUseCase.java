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

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;

@Service
public class ReorderWorkoutExercisesUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final Clock clock;

	public ReorderWorkoutExercisesUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public List<WorkoutExerciseResult> execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			List<UUID> exerciseIds) {
		AthleteRef athlete = WorkoutExerciseSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);

		if (exerciseIds == null || exerciseIds.isEmpty()) {
			throw new InvalidWorkoutExerciseOrderException("exerciseIds must not be empty");
		}
		if (exerciseIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidWorkoutExerciseOrderException("exerciseIds must not contain null values");
		}
		Set<UUID> unique = new HashSet<>();
		for (UUID id : exerciseIds) {
			if (!unique.add(id)) {
				throw new InvalidWorkoutExerciseOrderException("exerciseIds must not contain duplicates");
			}
		}

		List<WorkoutExercise> existing = workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(
				day.id(), athleteId);
		if (existing.size() != exerciseIds.size()) {
			throw new InvalidWorkoutExerciseOrderException("exerciseIds must include every workout exercise exactly once");
		}
		Map<UUID, WorkoutExercise> byId = existing.stream()
				.collect(Collectors.toMap(exercise -> exercise.id().value(), Function.identity()));
		for (UUID id : exerciseIds) {
			if (!byId.containsKey(id)) {
				throw new InvalidWorkoutExerciseOrderException("exerciseIds contains an unknown workout exercise");
			}
		}

		List<WorkoutExercise> ordered = exerciseIds.stream().map(byId::get).toList();
		WorkoutExerciseSupport.reassignOrders(ordered, workoutExerciseRepository, day.id(), athleteId, clock);
		return WorkoutExerciseSupport.toResults(
				workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId));
	}

}
