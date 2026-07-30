package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

@Service
public class ListTrainingEnvironmentsUseCase {
	private final AthleteContextPort athleteContextPort;
	private final TrainingEnvironmentRepository repository;

	public ListTrainingEnvironmentsUseCase(
			AthleteContextPort athleteContextPort,
			TrainingEnvironmentRepository repository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.repository = Objects.requireNonNull(repository);
	}

	@Transactional(readOnly = true)
	public TrainingEnvironmentResultPage execute(
			AccountId accountId,
			TrainingEnvironmentType type,
			java.util.List<EquipmentType> equipment,
			Boolean activeOnly,
			Integer page,
			Integer size) {
		AthleteRef athlete = TrainingEnvironmentSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int normalizedPage = TrainingEnvironmentSupport.normalizePage(page);
		int normalizedSize = TrainingEnvironmentSupport.normalizeSize(size);
		TrainingEnvironmentPage found = repository.findByAthlete(
				athleteId,
				TrainingEnvironmentFilters.of(type, equipment, activeOnly),
				normalizedPage,
				normalizedSize);
		return new TrainingEnvironmentResultPage(
				found.environments().stream().map(TrainingEnvironmentSupport::toResult).toList(),
				found.page(),
				found.size(),
				found.totalElements());
	}
}
