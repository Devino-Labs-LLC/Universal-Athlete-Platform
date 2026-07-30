package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

@Service
public class ArchiveTrainingEnvironmentUseCase {
	private final AthleteContextPort athleteContextPort;
	private final TrainingEnvironmentRepository repository;
	private final Clock clock;

	public ArchiveTrainingEnvironmentUseCase(
			AthleteContextPort athleteContextPort,
			TrainingEnvironmentRepository repository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, TrainingEnvironmentId environmentId) {
		AthleteRef athlete = TrainingEnvironmentSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingEnvironment environment = TrainingEnvironmentSupport.requireOwned(repository, athleteId, environmentId);
		if (!environment.active()) {
			return;
		}
		environment.archive(clock);
		repository.save(environment);
	}
}
