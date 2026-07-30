package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

@Service
public class GetTrainingEnvironmentUseCase {
	private final AthleteContextPort athleteContextPort;
	private final TrainingEnvironmentRepository repository;

	public GetTrainingEnvironmentUseCase(
			AthleteContextPort athleteContextPort,
			TrainingEnvironmentRepository repository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.repository = Objects.requireNonNull(repository);
	}

	@Transactional(readOnly = true)
	public TrainingEnvironmentResult execute(AccountId accountId, TrainingEnvironmentId environmentId) {
		AthleteRef athlete = TrainingEnvironmentSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return TrainingEnvironmentSupport.toResult(
				TrainingEnvironmentSupport.requireOwned(repository, athleteId, environmentId));
	}
}
