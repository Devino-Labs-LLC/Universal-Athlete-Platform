package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
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
import com.devinolabs.uap.training.domain.WorkoutDayStatus;

@Service
public class DeleteWorkoutDayUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final Clock clock;

	public DeleteWorkoutDayUseCase(
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
	public void execute(AccountId accountId, TrainingPlanId planId, WorkoutDayId dayId) {
		AthleteRef athlete = WorkoutDaySupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = workoutDayRepository
				.findByIdAndTrainingPlanIdAndAthleteId(dayId, plan.id(), athleteId)
				.orElseThrow(WorkoutDayNotFoundException::new);
		if (day.status() != WorkoutDayStatus.PLANNED) {
			throw new WorkoutDayDeleteNotAllowedException();
		}
		workoutDayRepository.delete(day);
		List<WorkoutDay> remaining = workoutDayRepository.findAllByTrainingPlanIdAndAthleteId(plan.id(), athleteId);
		WorkoutDaySupport.compactOrders(remaining, workoutDayRepository, plan.id(), athleteId, clock);
	}

}
