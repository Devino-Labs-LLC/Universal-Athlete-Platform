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
public class UpdateTrainingEnvironmentUseCase {
	private final AthleteContextPort athleteContextPort;
	private final TrainingEnvironmentRepository repository;
	private final Clock clock;

	public UpdateTrainingEnvironmentUseCase(
			AthleteContextPort athleteContextPort,
			TrainingEnvironmentRepository repository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingEnvironmentResult execute(
			AccountId accountId,
			TrainingEnvironmentId environmentId,
			UpdateTrainingEnvironmentCommand command) {
		AthleteRef athlete = TrainingEnvironmentSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingEnvironment environment = TrainingEnvironmentSupport.requireOwnedActive(
				repository, athleteId, environmentId);
		if (command.namePresent()) {
			if (command.name() == null || command.name().isBlank()) {
				throw new IllegalArgumentException("name must not be blank");
			}
			TrainingEnvironmentSupport.assertNoActiveDuplicate(
					repository, athleteId, command.name(), environment.id());
			environment.rename(command.name(), clock);
		}
		if (command.typePresent()) {
			if (command.type() == null) {
				throw new IllegalArgumentException("type must not be null");
			}
			environment.changeType(command.type(), clock);
		}
		if (command.availableEquipmentPresent()) {
			environment.replaceAvailableEquipment(
					command.availableEquipment() == null ? java.util.List.of() : command.availableEquipment(),
					clock);
		}
		if (command.descriptionPresent()) {
			environment.updateDescription(command.description(), clock);
		}
		if (command.facilityNotesPresent()) {
			environment.updateFacilityNotes(command.facilityNotes(), clock);
		}
		if (command.defaultEnvironmentPresent()) {
			TrainingEnvironmentSupport.applyDefaultSelection(
					repository, environment, Boolean.TRUE.equals(command.defaultEnvironment()), clock);
		}
		return TrainingEnvironmentSupport.toResult(repository.save(environment));
	}
}
