package com.devinolabs.uap.training.application;

import java.time.LocalDate;
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
public class GenerateWorkoutOccurrencesUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutOccurrenceGenerator workoutOccurrenceGenerator;

	public GenerateWorkoutOccurrencesUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutOccurrenceGenerator workoutOccurrenceGenerator) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutOccurrenceGenerator = Objects.requireNonNull(workoutOccurrenceGenerator);
	}

	@Transactional
	public WorkoutOccurrenceGenerationResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			LocalDate from,
			LocalDate to) {
		WorkoutOccurrenceGenerator.requireValidRange(from, to);
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		return workoutOccurrenceGenerator.generate(plan, athleteId, from, to);
	}

}
