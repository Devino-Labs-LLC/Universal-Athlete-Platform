package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@Service
public class ListTrainingPlansUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;

	public ListTrainingPlansUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
	}

	@Transactional(readOnly = true)
	public List<TrainingPlanResult> execute(
			AccountId accountId,
			TrainingPlanStatus status,
			TrainingPlanType planType) {
		AthleteRef athlete = TrainingPlanSupport.requireAthlete(athleteContextPort, accountId.value());
		return TrainingPlanSupport.toResults(trainingPlanRepository.findFiltered(
				AthleteId.of(athlete.athleteId()), status, planType));
	}

}
