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
public class CancelWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final Clock clock;

	public CancelWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutAdaptationProposalRepository proposalRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(AccountId accountId, WorkoutAdaptationProposalId proposalId) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutAdaptationProposal proposal = proposalRepository.lockOwnedById(proposalId, athleteId)
				.orElseThrow(WorkoutAdaptationProposalNotFoundException::new);
		proposal = WorkoutAdaptationProposalSupport.expireIfNeeded(proposalRepository, proposal, clock);
		if (!proposal.status().mutable()) {
			throw WorkoutAdaptationProposalSupport.terminalException(proposal.status());
		}
		proposal.cancel(clock);
		return WorkoutAdaptationProposalSupport.toResult(proposalRepository.save(proposal));
	}

}
