package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;

@Service
public class GetWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final Clock clock;

	public GetWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutAdaptationProposalRepository proposalRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(AccountId accountId, WorkoutAdaptationProposalId proposalId) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutAdaptationProposal proposal = WorkoutAdaptationProposalSupport.requireOwnedProposal(
				proposalRepository, proposalId, athleteId);
		proposal = WorkoutAdaptationProposalSupport.expireIfNeeded(proposalRepository, proposal, clock);
		return WorkoutAdaptationProposalSupport.toResult(proposal);
	}

}
