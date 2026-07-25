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
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@Service
public class GetWorkoutSessionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final WorkoutSessionRepository workoutSessionRepository;

	public GetWorkoutSessionUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			WorkoutSessionRepository workoutSessionRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.workoutSessionRepository = Objects.requireNonNull(workoutSessionRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutSessionResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId) {
		AthleteRef athlete = WorkoutSessionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutSessionSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutSessionSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutSessionSupport.requireOwnedExercise(workoutExerciseRepository, day.id(), athleteId, exerciseId);
		return WorkoutSessionSupport.toResult(
				WorkoutSessionSupport.requireSession(workoutSessionRepository, exerciseId, day.id(), athleteId));
	}

}
