package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
public class CreateWorkoutDayUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final Clock clock;

	public CreateWorkoutDayUseCase(
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
	public WorkoutDayResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			String title,
			String description,
			DayOfWeek scheduledDay,
			LocalTime plannedStartTime,
			Integer expectedDurationMinutes,
			Integer displayOrder) {
		AthleteRef athlete = WorkoutDaySupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);

		WorkoutDaySupport.assertUniqueTitle(workoutDayRepository, plan.id(), title, null);

		int order;
		int max = workoutDayRepository.findMaxDisplayOrder(plan.id(), athleteId);
		int size = max + 1;
		if (displayOrder == null) {
			order = size;
		}
		else {
			if (displayOrder < 0) {
				throw new InvalidWorkoutDayOrderException("displayOrder must not be negative");
			}
			order = Math.min(displayOrder, size);
			if (order < size) {
				WorkoutDaySupport.shiftOrdersUpFrom(workoutDayRepository, plan.id(), order, clock);
			}
		}

		try {
			WorkoutDay day = WorkoutDay.create(
					WorkoutDayId.generate(),
					plan.id(),
					athleteId,
					order,
					title,
					description,
					scheduledDay,
					plannedStartTime,
					expectedDurationMinutes,
					clock);
			return WorkoutDaySupport.toResult(workoutDayRepository.save(day));
		}
		catch (IllegalArgumentException ex) {
			throw WorkoutDaySupport.translateValidation(ex);
		}
	}

}
