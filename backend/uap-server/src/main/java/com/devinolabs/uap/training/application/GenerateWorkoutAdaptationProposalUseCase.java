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
public class GenerateWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutAdaptationProposalGenerationService generationService;

	public GenerateWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutAdaptationProposalGenerationService generationService) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.generationService = Objects.requireNonNull(generationService);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			Integer suggestionLimit,
			Boolean includeAlternatives,
			Integer expirationMinutes) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requireMutablePlan(
				trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		return generationService.generate(
				athleteId,
				plan,
				day,
				occurrence,
				suggestionLimit,
				includeAlternatives,
				expirationMinutes,
				null);
	}

}
