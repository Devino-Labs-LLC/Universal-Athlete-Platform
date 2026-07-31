package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
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
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationFeasibilityFingerprint;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class ApplyWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final WorkoutFeasibilityAnalyzer feasibilityAnalyzer;
	private final Clock clock;

	public ApplyWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			WorkoutAdaptationProposalRepository proposalRepository,
			WorkoutExerciseSubstitutionHistoryRepository substitutionHistoryRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			WorkoutFeasibilityAnalyzer feasibilityAnalyzer,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.substitutionHistoryRepository = Objects.requireNonNull(substitutionHistoryRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.feasibilityAnalyzer = Objects.requireNonNull(feasibilityAnalyzer);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutAdaptationApplicationResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutAdaptationProposalId proposalId,
			long expectedProposalVersion) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requireMutablePlan(
				trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);

		WorkoutAdaptationProposal proposal = proposalRepository.lockOwnedById(proposalId, athleteId)
				.orElseThrow(WorkoutAdaptationProposalNotFoundException::new);
		if (!proposal.workoutOccurrenceId().equals(occurrenceId)) {
			throw new WorkoutAdaptationProposalItemMismatchException();
		}
		proposal = WorkoutAdaptationProposalSupport.expireIfNeeded(proposalRepository, proposal, clock);
		if (!proposal.status().mutable()) {
			throw WorkoutAdaptationProposalSupport.terminalException(proposal.status());
		}
		if (proposal.version() != expectedProposalVersion) {
			throw new WorkoutAdaptationProposalVersionConflictException();
		}

		List<WorkoutExerciseExecution> executions = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByOccurrenceIdAndAthleteId(
				occurrence.id(), athleteId);
		try {
			WorkoutExerciseExecutionBulkSubstitutionSupport.requireSelectedSubstitutionsSubstitutable(
					proposal, executions, sets);
		}
		catch (WorkoutExerciseSubstitutionLockedException ex) {
			throw new WorkoutAdaptationProposalLockedException();
		}
		FeasibilityEnvironmentContextResult environmentContext =
				WorkoutFeasibilitySupport.resolveOccurrenceEnvironment(occurrence);
		Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById =
				WorkoutAdaptationProposalGenerator.definitionsForExecutions(
						exerciseDefinitionRepository, executions, proposal.items());
		WorkoutAdaptationFeasibilityFingerprint currentFingerprint = WorkoutAdaptationProposalSupport.buildFingerprint(
				occurrence,
				environmentContext,
				executions,
				sets,
				proposal,
				definitionsById,
				relationshipRepository,
				athleteId);
		if (!currentFingerprint.value().equals(proposal.feasibilityFingerprint().value())) {
			proposal.markStale(clock);
			proposalRepository.save(proposal);
			throw new WorkoutAdaptationProposalStaleException();
		}
		if (!proposal.isReadyForApply()) {
			if (proposal.hasPendingOrUnresolvedItems()) {
				throw new WorkoutAdaptationProposalUnresolvedException();
			}
			throw new WorkoutAdaptationProposalUnresolvedException();
		}

		List<WorkoutExerciseExecutionBulkSubstitutionSupport.AppliedSubstitution> applied;
		try {
			applied = WorkoutExerciseExecutionBulkSubstitutionSupport.applySelectedSubstitutions(
					proposal,
					executions,
					sets,
					exerciseDefinitionRepository,
					relationshipRepository,
					workoutExerciseExecutionRepository,
					substitutionHistoryRepository,
					occurrence,
					athleteId,
					clock);
		}
		catch (WorkoutExerciseSubstitutionLockedException ex) {
			throw new WorkoutAdaptationProposalLockedException();
		}

		proposal.markApplied(clock);
		WorkoutAdaptationProposal saved = proposalRepository.save(proposal);

		List<WorkoutExerciseExecution> refreshedExecutions = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
		WorkoutOccurrenceFeasibilityResult finalFeasibility = feasibilityAnalyzer.analyzeOccurrence(
				plan,
				day,
				occurrence,
				athleteId,
				environmentContext,
				refreshedExecutions,
				WorkoutFeasibilitySupport.DEFAULT_SUGGESTION_LIMIT,
				false);

		return toApplicationResult(saved, applied, finalFeasibility, environmentContext);
	}

	private WorkoutAdaptationApplicationResult toApplicationResult(
			WorkoutAdaptationProposal proposal,
			List<WorkoutExerciseExecutionBulkSubstitutionSupport.AppliedSubstitution> applied,
			WorkoutOccurrenceFeasibilityResult finalFeasibility,
			FeasibilityEnvironmentContextResult environmentContext) {
		List<WorkoutAdaptationAppliedItemResult> appliedItems = new ArrayList<>();
		List<WorkoutAdaptationAppliedItemResult> excludedItems = new ArrayList<>();
		WorkoutAdaptationEnvironmentContextResult adaptationEnvironment = new WorkoutAdaptationEnvironmentContextResult(
				environmentContext.contextSource(),
				environmentContext.trainingEnvironmentId(),
				environmentContext.trainingEnvironmentName(),
				environmentContext.availableEquipment());
		for (WorkoutAdaptationProposalItem item : proposal.items()) {
			if (item.action() == WorkoutAdaptationAction.EXCLUDED
					&& item.athleteDecision() == WorkoutAdaptationDecision.REJECTED) {
				excludedItems.add(new WorkoutAdaptationAppliedItemResult(
						item.workoutExerciseExecutionId(),
						item.id(),
						item.currentPerformedExerciseDefinitionId(),
						null,
						null,
						item.athleteDecision(),
						null,
						adaptationEnvironment));
			}
		}
		for (WorkoutExerciseExecutionBulkSubstitutionSupport.AppliedSubstitution substitution : applied) {
			appliedItems.add(new WorkoutAdaptationAppliedItemResult(
					substitution.execution().id(),
					substitution.item().id(),
					substitution.item().currentPerformedExerciseDefinitionId(),
					substitution.execution().performedExerciseDefinitionId(),
					substitution.item().effectiveRelationshipId().orElse(null),
					substitution.item().athleteDecision(),
					substitution.history().id(),
					adaptationEnvironment));
		}
		int unchanged = (int) proposal.items().stream()
				.filter(item -> item.action() == WorkoutAdaptationAction.NO_CHANGE)
				.count();
		return new WorkoutAdaptationApplicationResult(
				proposal.id(),
				WorkoutAdaptationProposalStatus.APPLIED,
				proposal.appliedAt(),
				appliedItems.size(),
				unchanged,
				excludedItems.size(),
				finalFeasibility,
				List.copyOf(appliedItems),
				List.copyOf(excludedItems));
	}

}
