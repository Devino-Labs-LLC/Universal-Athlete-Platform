package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationOccurrenceContext;
import com.devinolabs.uap.training.domain.WorkoutAdaptationRecommendationContext;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Generates an athlete-reviewed adaptation proposal from an immutable training recommendation.
 * Does not apply the proposal.
 */
@Service
public class GenerateRecommendedWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutAdaptationProposalGenerationService generationService;

	public GenerateRecommendedWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			DailyTrainingRecommendationRepository recommendationRepository,
			DailyReadinessAssessmentRepository assessmentRepository,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutAdaptationProposalGenerationService generationService) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.generationService = Objects.requireNonNull(generationService);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(
			AccountId accountId,
			UUID recommendationId,
			UUID workoutOccurrenceId,
			Integer suggestionLimit,
			Boolean includeAlternatives,
			Integer expirationMinutes) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyTrainingRecommendation recommendation = recommendationRepository
				.findByIdAndAthleteId(DailyTrainingRecommendationId.of(recommendationId), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationNotFoundException(
						"Daily training recommendation not found: " + recommendationId));

		if (recommendation.overallAction() != TrainingRecommendationAction.MODIFY_SESSION) {
			throw new TrainingRecommendationNotAdaptationEligibleException(
					"Only MODIFY_SESSION recommendations can generate adaptation proposals");
		}

		TrainingRecommendationOccurrenceContext occurrenceContext = recommendation.occurrenceContexts().stream()
				.filter(context -> context.occurrenceId().equals(workoutOccurrenceId))
				.findFirst()
				.orElseThrow(() -> new TrainingRecommendationOccurrenceMismatchException(
						"Occurrence is not part of the recommendation source snapshot context"));

		DailyReadinessAssessment assessment = assessmentRepository
				.findByIdAndAthleteId(recommendation.dailyReadinessAssessmentId(), athleteId)
				.orElseThrow(() -> new RecommendedAdaptationGenerationFailedException(
						"Source readiness assessment missing for recommendation " + recommendationId));

		TrainingPlan plan = WorkoutExerciseExecutionSupport.requireMutablePlan(
				trainingPlanRepository, athleteId, TrainingPlanId.of(occurrenceContext.trainingPlanId()));
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, WorkoutDayId.of(occurrenceContext.workoutDayId()));
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository,
				WorkoutOccurrenceId.of(workoutOccurrenceId),
				day.id(),
				athleteId);

		WorkoutAdaptationRecommendationContext recommendationContext =
				WorkoutAdaptationRecommendationContext.from(recommendation, assessment);

		try {
			return generationService.generate(
					athleteId,
					plan,
					day,
					occurrence,
					suggestionLimit,
					includeAlternatives,
					expirationMinutes,
					recommendationContext);
		}
		catch (ActiveWorkoutAdaptationProposalExistsException
				| RecommendedAdaptationOccurrenceLockedException
				| RecommendedAdaptationOccurrenceNotEligibleException
				| TrainingRecommendationNotAdaptationEligibleException
				| TrainingRecommendationOccurrenceMismatchException
				| WorkoutAdaptationProposalEnvironmentRequiredException
				| InvalidAdaptationProposalExpirationException
				| InvalidFeasibilitySuggestionLimitException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new RecommendedAdaptationGenerationFailedException(
					"Failed to generate recommendation-based adaptation proposal", ex);
		}
	}

}
