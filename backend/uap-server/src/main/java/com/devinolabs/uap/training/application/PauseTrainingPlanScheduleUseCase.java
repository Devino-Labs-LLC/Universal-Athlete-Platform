package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;

@Service
public class PauseTrainingPlanScheduleUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final Clock clock;

	public PauseTrainingPlanScheduleUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingPlanResult execute(AccountId accountId, TrainingPlanId planId) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		try {
			plan.pauseSchedule(clock);
		}
		catch (IllegalStateException ex) {
			throw TrainingScheduleSupport.translateScheduleState(ex);
		}
		return TrainingPlanSupport.toResult(trainingPlanRepository.save(plan));
	}

}
