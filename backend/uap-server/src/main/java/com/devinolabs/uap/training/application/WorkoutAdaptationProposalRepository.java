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

	WorkoutAdaptationProposalPage findByAthlete(
			AthleteId athleteId,
			WorkoutAdaptationProposalFilters filters,
			int page,
			int size);

}
