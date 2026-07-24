package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;

@Service
public class DeleteTrainingPlanUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;

	public DeleteTrainingPlanUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
	}

	@Transactional
	public void execute(AccountId accountId, TrainingPlanId planId) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		TrainingPlan plan = trainingPlanRepository
				.findByIdAndAthleteId(planId, AthleteId.of(athlete.athleteId()))
				.orElseThrow(TrainingPlanNotFoundException::new);
		if (plan.status() != TrainingPlanStatus.DRAFT) {
			throw new TrainingPlanDeleteNotAllowedException();
		}
		trainingPlanRepository.delete(plan);
	}

}
