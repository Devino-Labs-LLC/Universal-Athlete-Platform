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

@Service
public class ReorderWorkoutDaysUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final Clock clock;

	public ReorderWorkoutDaysUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public List<WorkoutDayResult> execute(AccountId accountId, TrainingPlanId planId, List<UUID> dayIds) {
		AthleteRef athlete = WorkoutDaySupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);

		if (dayIds == null || dayIds.isEmpty()) {
			throw new InvalidWorkoutDayOrderException("dayIds must not be empty");
		}
		if (dayIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidWorkoutDayOrderException("dayIds must not contain null values");
		}
		Set<UUID> unique = new HashSet<>();
		for (UUID id : dayIds) {
			if (!unique.add(id)) {
				throw new InvalidWorkoutDayOrderException("dayIds must not contain duplicates");
			}
		}

		List<WorkoutDay> existing = workoutDayRepository.findAllByTrainingPlanIdAndAthleteId(plan.id(), athleteId);
		if (existing.size() != dayIds.size()) {
			throw new InvalidWorkoutDayOrderException("dayIds must include every workout day exactly once");
		}
		Map<UUID, WorkoutDay> byId = existing.stream()
				.collect(Collectors.toMap(day -> day.id().value(), Function.identity()));
		for (UUID id : dayIds) {
			if (!byId.containsKey(id)) {
				throw new InvalidWorkoutDayOrderException("dayIds contains an unknown workout day");
			}
		}

		List<WorkoutDay> ordered = dayIds.stream().map(byId::get).toList();
		WorkoutDaySupport.reassignOrders(ordered, workoutDayRepository, plan.id(), athleteId, clock);
		return WorkoutDaySupport.toResults(
				workoutDayRepository.findAllByTrainingPlanIdAndAthleteId(plan.id(), athleteId));
	}

}
