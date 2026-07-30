package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

@Service
public class CreateTrainingEnvironmentUseCase {
	private final AthleteContextPort athleteContextPort;
	private final TrainingEnvironmentRepository repository;
	private final Clock clock;

	public CreateTrainingEnvironmentUseCase(AthleteContextPort athleteContextPort, TrainingEnvironmentRepository repository, Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingEnvironmentResult execute(AccountId accountId, String name, TrainingEnvironmentType type,
			Collection<EquipmentType> availableEquipment, String description, String facilityNotes, Boolean defaultEnvironment) {
		AthleteRef athlete = TrainingEnvironmentSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingEnvironmentSupport.assertNoActiveDuplicate(repository, athleteId, name, null);
		boolean makeDefault = Boolean.TRUE.equals(defaultEnvironment) || !repository.hasAnyActiveByAthleteId(athleteId);
		if (makeDefault) repository.clearDefaultForAthleteExcept(athleteId, null);
		TrainingEnvironment env = TrainingEnvironment.create(TrainingEnvironmentId.generate(), athleteId, name, type,
				availableEquipment, description, facilityNotes, makeDefault, clock);
		return TrainingEnvironmentSupport.toResult(repository.save(env));
	}
}
