package com.devinolabs.uap.training.application;

import java.util.Objects;

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

@Service
public class GetWorkoutDayUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;

	public GetWorkoutDayUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutDayResult execute(AccountId accountId, TrainingPlanId planId, WorkoutDayId dayId) {
		AthleteRef athlete = WorkoutDaySupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = workoutDayRepository
				.findByIdAndTrainingPlanIdAndAthleteId(dayId, plan.id(), athleteId)
				.orElseThrow(WorkoutDayNotFoundException::new);
		return WorkoutDaySupport.toResult(day);
	}

}
