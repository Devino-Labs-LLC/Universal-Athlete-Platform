package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationFeasibilityFingerprint;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class GenerateWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public GenerateWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutAdaptationProposalRepository proposalRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
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

		proposalRepository.findActiveByOccurrenceId(occurrenceId, athleteId)
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
		WorkoutAdaptationProposalSupport.requireSubstitutableOccurrence(occurrence, executions, sets);

		int resolvedLimit = WorkoutFeasibilitySupport.resolveSuggestionLimit(suggestionLimit);
		boolean resolvedIncludeAlternatives = Boolean.TRUE.equals(includeAlternatives);
		int resolvedExpiration = WorkoutAdaptationProposalSupport.resolveExpirationMinutes(expirationMinutes);

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
				clock);
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
				clock);
		return WorkoutAdaptationProposalSupport.toResult(proposalRepository.save(proposal));
	}

}
