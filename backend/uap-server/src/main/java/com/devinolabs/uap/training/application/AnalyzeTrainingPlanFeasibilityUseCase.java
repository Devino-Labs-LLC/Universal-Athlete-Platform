package com.devinolabs.uap.training.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatusResolver;

@Service
public class AnalyzeTrainingPlanFeasibilityUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;
	private final WorkoutFeasibilityAnalyzer feasibilityAnalyzer;

	public AnalyzeTrainingPlanFeasibilityUseCase(
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
	public TrainingPlanFeasibilityResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			TrainingEnvironmentId trainingEnvironmentId,
			Boolean usePreferredEnvironments,
			Integer suggestionLimit,
			Boolean includeAlternatives) {
		WorkoutFeasibilitySupport.assertEnvironmentMode(trainingEnvironmentId, usePreferredEnvironments);
		AthleteRef athlete = WorkoutDaySupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requirePlan(trainingPlanRepository, athleteId, planId);
		int resolvedLimit = WorkoutFeasibilitySupport.resolveSuggestionLimit(suggestionLimit);
		boolean resolvedIncludeAlternatives = Boolean.TRUE.equals(includeAlternatives);
		List<WorkoutDay> days = workoutDayRepository.findAllByTrainingPlanIdAndAthleteId(plan.id(), athleteId);
		List<WorkoutDayFeasibilityResult> daySummaries = new ArrayList<>();
		int totalExercises = 0;
		int feasibleExercises = 0;
		int exercisesWithoutContext = 0;
		int analyzableExercises = 0;
		for (WorkoutDay day : days) {
			FeasibilityEnvironmentContextResult environmentContext = Boolean.TRUE.equals(usePreferredEnvironments)
					? WorkoutFeasibilitySupport.resolvePreferredEnvironment(
							trainingEnvironmentRepository, day, plan, athleteId)
					: WorkoutFeasibilitySupport.resolveExplicitEnvironment(
							trainingEnvironmentRepository, athleteId, trainingEnvironmentId);
			List<WorkoutExercise> exercises = workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId);
			WorkoutDayFeasibilityResult dayResult = feasibilityAnalyzer.analyzeDay(
					plan,
					day,
					athleteId,
					environmentContext,
					exercises,
					resolvedLimit,
					resolvedIncludeAlternatives);
			daySummaries.add(dayResult);
		}
		for (WorkoutDayFeasibilityResult dayResult : daySummaries) {
			for (ExerciseFeasibilityAnalysisResult exercise : dayResult.exercises()) {
				totalExercises++;
				if (dayResult.environmentContext() == null
						|| exercise.reasonCode() == com.devinolabs.uap.training.domain.FeasibilityReasonCode.NO_ENVIRONMENT_CONTEXT) {
					exercisesWithoutContext++;
					continue;
				}
				if (exercise.currentStatus() == ExerciseFeasibilityStatus.NOT_ANALYZABLE) {
					continue;
				}
				analyzableExercises++;
				if (exercise.feasible()) {
					feasibleExercises++;
				}
			}
		}
		WorkoutFeasibilityStatus status = WorkoutFeasibilityStatusResolver.resolvePlan(
				totalExercises, feasibleExercises, exercisesWithoutContext, analyzableExercises);
		return new TrainingPlanFeasibilityResult(
				plan.id(),
				plan.name(),
				status,
				totalExercises,
				feasibleExercises,
				Math.max(0, analyzableExercises - feasibleExercises),
				exercisesWithoutContext,
				analyzableExercises,
				WorkoutFeasibilityStatusResolver.percentage(totalExercises, feasibleExercises),
				daySummaries);
	}

}
