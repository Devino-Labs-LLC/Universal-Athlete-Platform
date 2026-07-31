package com.devinolabs.uap.training.application;

import java.time.Clock;
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
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;

@Service
public class UpdateWorkoutAdaptationProposalItemUseCase {

	private final AthleteContextPort athleteContextPort;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public UpdateWorkoutAdaptationProposalItemUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutAdaptationProposalRepository proposalRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(
			AccountId accountId,
			WorkoutAdaptationProposalId proposalId,
			WorkoutAdaptationProposalItemId itemId,
			WorkoutAdaptationDecision decision,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId,
			String athleteNotes) {
		Objects.requireNonNull(decision, "decision must not be null");
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutAdaptationProposal proposal = proposalRepository.lockOwnedById(proposalId, athleteId)
				.orElseThrow(WorkoutAdaptationProposalNotFoundException::new);
		proposal = WorkoutAdaptationProposalSupport.expireIfNeeded(proposalRepository, proposal, clock);
		if (!proposal.status().mutable()) {
			throw WorkoutAdaptationProposalSupport.terminalException(proposal.status());
		}
		WorkoutAdaptationProposalItem item = findItem(proposal, itemId);
		applyDecision(
				proposal,
				item,
				decision,
				targetExerciseDefinitionId,
				substitutionRelationshipId,
				athleteNotes,
				athleteId);
		proposal.refreshSummary();
		proposal.refreshStatus();
		return WorkoutAdaptationProposalSupport.toResult(proposalRepository.save(proposal));
	}

	private WorkoutAdaptationProposalItem findItem(
			WorkoutAdaptationProposal proposal,
			WorkoutAdaptationProposalItemId itemId) {
		try {
			return proposal.requireItem(itemId);
		}
		catch (IllegalArgumentException ex) {
			throw new WorkoutAdaptationProposalItemNotFoundException();
		}
	}

	private void applyDecision(
			WorkoutAdaptationProposal proposal,
			WorkoutAdaptationProposalItem item,
			WorkoutAdaptationDecision decision,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId,
			String athleteNotes,
			AthleteId athleteId) {
		switch (decision) {
			case ACCEPTED -> item.acceptDefault(clock);
			case OVERRIDDEN -> {
				if (targetExerciseDefinitionId == null) {
					throw new InvalidWorkoutAdaptationDecisionException(
							"targetExerciseDefinitionId is required for OVERRIDDEN");
				}
				ExerciseDefinition target = resolveTarget(athleteId, targetExerciseDefinitionId);
				WorkoutAdaptationProposalSupport.validateTargetEnvironmentCompatible(
						target, proposal.availableEquipmentSnapshot());
				ExerciseSubstitutionRelationship relationship = resolveRelationship(
						athleteId,
						item,
						target.id(),
						substitutionRelationshipId);
				item.overrideTarget(target.id(), relationship == null ? null : relationship.id(), athleteNotes, clock);
			}
			case REJECTED -> item.reject(athleteNotes, clock);
			case PENDING -> item.resetToPending(clock);
			case NOT_REQUIRED -> throw new InvalidWorkoutAdaptationDecisionException();
		}
	}

	private ExerciseDefinition resolveTarget(AthleteId athleteId, ExerciseDefinitionId targetExerciseDefinitionId) {
		try {
			return ExerciseDefinitionAccessPolicy.requireSelectable(
					athleteId,
					ExerciseDefinitionSupport.requireAccessible(
							exerciseDefinitionRepository, athleteId, targetExerciseDefinitionId));
		}
		catch (ExerciseDefinitionNotFoundException | ExerciseDefinitionNotAccessibleException
				| ExerciseDefinitionArchivedException ex) {
			throw new AdaptationTargetNotAccessibleException();
		}
	}

	private ExerciseSubstitutionRelationship resolveRelationship(
			AthleteId athleteId,
			WorkoutAdaptationProposalItem item,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipId substitutionRelationshipId) {
		if (substitutionRelationshipId == null) {
			return null;
		}
		ExerciseSubstitutionRelationship relationship = relationshipRepository
				.findActiveById(substitutionRelationshipId)
				.orElseThrow(ExerciseSubstitutionRelationshipNotFoundException::new);
		if (!ExerciseSubstitutionRelationshipAccessPolicy.isAccessible(athleteId, relationship)) {
			throw new ExerciseSubstitutionRelationshipNotAccessibleException();
		}
		if (!relationship.sourceExerciseDefinitionId().equals(item.currentPerformedExerciseDefinitionId())) {
			throw new AdaptationRelationshipMismatchException(
					"Substitution relationship source must match the currently performed exercise");
		}
		if (!relationship.targetExerciseDefinitionId().equals(targetExerciseDefinitionId)) {
			throw new AdaptationRelationshipMismatchException(
					"Substitution relationship target must match the requested substitute exercise");
		}
		return relationship;
	}

}
