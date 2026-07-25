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
public class GetWorkoutExerciseUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;

	public GetWorkoutExerciseUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutExerciseResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId) {
		AthleteRef athlete = WorkoutExerciseSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		return WorkoutExerciseSupport.toResult(workoutExerciseRepository
				.findByIdAndWorkoutDayIdAndAthleteId(exerciseId, day.id(), athleteId)
				.orElseThrow(WorkoutExerciseNotFoundException::new));
	}

}
