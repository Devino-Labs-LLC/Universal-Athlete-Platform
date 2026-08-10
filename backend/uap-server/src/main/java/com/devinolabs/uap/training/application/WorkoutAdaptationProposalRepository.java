package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public interface WorkoutAdaptationProposalRepository {

	WorkoutAdaptationProposal save(WorkoutAdaptationProposal proposal);

	Optional<WorkoutAdaptationProposal> findOwnedById(WorkoutAdaptationProposalId id, AthleteId athleteId);

	Optional<WorkoutAdaptationProposal> lockOwnedById(WorkoutAdaptationProposalId id, AthleteId athleteId);

	Optional<WorkoutAdaptationProposal> findActiveByOccurrenceId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	/**
	 * Scalar header lookup for client facades — avoids EAGER item/equipment hydration.
	 */
	Optional<WorkoutAdaptationProposalBrief> findActiveBriefByOccurrenceId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	/**
	 * Scalar outstanding-proposal headers for overview — one query, no child hydration.
	 * Statuses: DRAFT, READY, PARTIALLY_RESOLVED. Ordered by generatedAt DESC, id ASC.
	 */
	List<WorkoutAdaptationProposalOutstandingBrief> findOutstandingBriefsByAthlete(
			AthleteId athleteId,
			int limit);

	WorkoutAdaptationProposalPage findByAthlete(
			AthleteId athleteId,
			WorkoutAdaptationProposalFilters filters,
			int page,
			int size);

}
