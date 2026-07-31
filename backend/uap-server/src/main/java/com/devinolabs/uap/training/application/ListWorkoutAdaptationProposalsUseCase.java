package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class ListWorkoutAdaptationProposalsUseCase {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final AthleteContextPort athleteContextPort;
	private final WorkoutAdaptationProposalRepository proposalRepository;

	public ListWorkoutAdaptationProposalsUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutAdaptationProposalRepository proposalRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
	}

	@Transactional(readOnly = true)
	public List<WorkoutAdaptationProposalSummaryResult> execute(
			AccountId accountId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutAdaptationProposalStatus status,
			Integer page,
			Integer size) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		WorkoutAdaptationProposalPage found = proposalRepository.findByAthlete(
				athleteId,
				WorkoutAdaptationProposalFilters.of(occurrenceId, status),
				resolvedPage,
				resolvedSize);
		return found.proposals().stream()
				.map(WorkoutAdaptationProposalSupport::toSummaryResult)
				.toList();
	}

}
