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
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class GetWorkoutOccurrenceUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;

	public GetWorkoutOccurrenceUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutOccurrenceDetailResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
		AthleteRef athlete = WorkoutOccurrenceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutOccurrenceSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutOccurrenceSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		return WorkoutOccurrenceSupport.toDetailResult(
				occurrence,
				WorkoutExerciseExecutionSupport.toResults(
						workoutExerciseExecutionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(
								occurrence.id(), athleteId),
						workoutExerciseSetRepository,
						athleteId));
	}

}
