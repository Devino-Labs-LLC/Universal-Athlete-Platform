package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class RegenerateWorkoutAdaptationProposalUseCase {

	private final AthleteContextPort athleteContextPort;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase;
	private final Clock clock;

	public RegenerateWorkoutAdaptationProposalUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutAdaptationProposalRepository proposalRepository,
			GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.generateWorkoutAdaptationProposalUseCase = Objects.requireNonNull(generateWorkoutAdaptationProposalUseCase);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutAdaptationProposalResult execute(
			AccountId accountId,
			WorkoutAdaptationProposalId proposalId,
			Integer suggestionLimit,
			Boolean includeAlternatives,
			Integer expirationMinutes) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutAdaptationProposal existing = proposalRepository.lockOwnedById(proposalId, athleteId)
				.orElseThrow(WorkoutAdaptationProposalNotFoundException::new);
		existing = WorkoutAdaptationProposalSupport.expireIfNeeded(proposalRepository, existing, clock);
		if (!existing.status().mutable()) {
			throw WorkoutAdaptationProposalSupport.terminalException(existing.status());
		}
		TrainingPlanId planId = existing.trainingPlanId();
		WorkoutDayId dayId = existing.workoutDayId();
		WorkoutOccurrenceId occurrenceId = existing.workoutOccurrenceId();
		existing.cancel(clock);
		proposalRepository.save(existing);
		return generateWorkoutAdaptationProposalUseCase.execute(
				accountId,
				planId,
				dayId,
				occurrenceId,
				suggestionLimit,
				includeAlternatives,
				expirationMinutes);
	}

}
