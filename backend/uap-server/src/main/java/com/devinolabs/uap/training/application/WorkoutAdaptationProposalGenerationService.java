package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.WorkoutAdaptationFeasibilityFingerprint;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutAdaptationRecommendationContext;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;

/**
 * Shared proposal generation for manual and recommendation-origin flows.
 */
@Service
public class WorkoutAdaptationProposalGenerationService {

	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public WorkoutAdaptationProposalGenerationService(
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutAdaptationProposalRepository proposalRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	public WorkoutAdaptationProposalResult generate(
			AthleteId athleteId,
			TrainingPlan plan,
			WorkoutDay day,
			WorkoutOccurrence occurrence,
			Integer suggestionLimit,
			Boolean includeAlternatives,
			Integer expirationMinutes,
			WorkoutAdaptationRecommendationContext recommendationContext) {
		proposalRepository.findActiveByOccurrenceId(occurrence.id(), athleteId)
				.ifPresent(existing -> {
					throw new ActiveWorkoutAdaptationProposalExistsException();
				});

		FeasibilityEnvironmentContextResult environmentContext =
				WorkoutFeasibilitySupport.resolveOccurrenceEnvironment(occurrence);
		WorkoutAdaptationProposalSupport.requireEnvironmentContext(environmentContext);

		List<WorkoutExerciseExecution> executions = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByOccurrenceIdAndAthleteId(
				occurrence.id(), athleteId);
		try {
			WorkoutAdaptationProposalSupport.requireSubstitutableOccurrence(occurrence, executions, sets);
		}
		catch (WorkoutExerciseSubstitutionLockedException ex) {
			if (recommendationContext != null) {
				throw new RecommendedAdaptationOccurrenceLockedException(ex.getMessage(), ex);
			}
			throw ex;
		}
		catch (InvalidWorkoutOccurrenceStatusException | InvalidWorkoutExerciseExecutionStatusException ex) {
			if (recommendationContext != null) {
				throw new RecommendedAdaptationOccurrenceNotEligibleException(ex.getMessage(), ex);
			}
			throw ex;
		}

		int resolvedLimit = WorkoutFeasibilitySupport.resolveSuggestionLimit(suggestionLimit);
		boolean resolvedIncludeAlternatives = Boolean.TRUE.equals(includeAlternatives);
		int resolvedExpiration = WorkoutAdaptationProposalSupport.resolveExpirationMinutes(expirationMinutes);
		boolean preferLowerImpact = recommendationContext != null
				&& recommendationContext.preferLowerImpactVariations();

		WorkoutAdaptationProposalId proposalId = WorkoutAdaptationProposalId.generate();
		Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource =
				WorkoutAdaptationProposalGenerator.relationshipsForExecutions(
						relationshipRepository, executions, athleteId);
		Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById =
				WorkoutAdaptationProposalGenerator.definitionsForExecutions(
						exerciseDefinitionRepository, executions, relationshipsBySource);
		List<WorkoutAdaptationProposalItem> items = WorkoutAdaptationProposalGenerator.generateItems(
				proposalId,
				athleteId,
				executions,
				environmentContext,
				definitionsById,
				relationshipsBySource,
				resolvedLimit,
				resolvedIncludeAlternatives,
				clock,
				preferLowerImpact);
		definitionsById = WorkoutAdaptationProposalGenerator.definitionsForExecutions(
				exerciseDefinitionRepository, executions, items);
		WorkoutAdaptationFeasibilityFingerprint fingerprint = WorkoutAdaptationProposalSupport.buildFingerprint(
				occurrence,
				environmentContext,
				executions,
				sets,
				items,
				definitionsById,
				relationshipRepository,
				athleteId);

		WorkoutAdaptationProposal proposal = WorkoutAdaptationProposal.generate(
				proposalId,
				athleteId,
				plan.id(),
				day.id(),
				occurrence,
				environmentContext.contextSource(),
				environmentContext.trainingEnvironmentId(),
				environmentContext.trainingEnvironmentName(),
				environmentContext.availableEquipment(),
				fingerprint,
				items,
				resolvedExpiration,
				clock,
				recommendationContext);
		try {
			return WorkoutAdaptationProposalSupport.toResult(proposalRepository.save(proposal));
		}
		catch (DataIntegrityViolationException ex) {
			if (indicatesActiveProposalConflict(ex)) {
				throw new ActiveWorkoutAdaptationProposalExistsException();
			}
			throw ex;
		}
	}

	private static boolean indicatesActiveProposalConflict(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
		return message != null && message.contains("uq_adaptation_proposals_active_occurrence");
	}

}
