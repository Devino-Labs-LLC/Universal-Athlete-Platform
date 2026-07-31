package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;

@Service
public class AnalyzeWorkoutDayFeasibilityUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;
	private final WorkoutFeasibilityAnalyzer feasibilityAnalyzer;

	public AnalyzeWorkoutDayFeasibilityUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			TrainingEnvironmentRepository trainingEnvironmentRepository,
			WorkoutFeasibilityAnalyzer feasibilityAnalyzer) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.trainingEnvironmentRepository = Objects.requireNonNull(trainingEnvironmentRepository);
		this.feasibilityAnalyzer = Objects.requireNonNull(feasibilityAnalyzer);
	}

	@Transactional(readOnly = true)
	public WorkoutDayFeasibilityResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			TrainingEnvironmentId trainingEnvironmentId,
			Integer suggestionLimit,
			Boolean includeAlternatives) {
		AthleteRef athlete = WorkoutDaySupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = workoutDayRepository
				.findByIdAndTrainingPlanIdAndAthleteId(dayId, plan.id(), athleteId)
				.orElseThrow(WorkoutDayNotFoundException::new);
		int resolvedLimit = WorkoutFeasibilitySupport.resolveSuggestionLimit(suggestionLimit);
		boolean resolvedIncludeAlternatives = Boolean.TRUE.equals(includeAlternatives);
		FeasibilityEnvironmentContextResult environmentContext = WorkoutFeasibilitySupport.resolveExplicitEnvironment(
				trainingEnvironmentRepository, athleteId, trainingEnvironmentId);
		List<WorkoutExercise> exercises = workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId);
		return feasibilityAnalyzer.analyzeDay(
				plan,
				day,
				athleteId,
				environmentContext,
				exercises,
				resolvedLimit,
				resolvedIncludeAlternatives);
	}

}
