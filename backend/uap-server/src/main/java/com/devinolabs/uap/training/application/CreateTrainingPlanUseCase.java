package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteGoalId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.AthleteSportId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@Service
public class CreateTrainingPlanUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;
	private final Clock clock;

	public CreateTrainingPlanUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			TrainingEnvironmentRepository trainingEnvironmentRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.trainingEnvironmentRepository = Objects.requireNonNull(trainingEnvironmentRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingPlanResult execute(
			AccountId accountId,
			TrainingPlanType type,
			String customTypeName,
			String name,
			String description,
			LocalDate startDate,
			LocalDate endDate,
			UUID athleteSportId,
			UUID athleteGoalId) {
		return execute(
				accountId,
				type,
				customTypeName,
				name,
				description,
				startDate,
				endDate,
				athleteSportId,
				athleteGoalId,
				null);
	}

	@Transactional
	public TrainingPlanResult execute(
			AccountId accountId,
			TrainingPlanType type,
			String customTypeName,
			String name,
			String description,
			LocalDate startDate,
			LocalDate endDate,
			UUID athleteSportId,
			UUID athleteGoalId,
			UUID defaultTrainingEnvironmentId) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		TrainingPlanSupport.assertLinks(athleteContextPort, athlete, athleteSportId, athleteGoalId);
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlanSupport.assertNoDuplicate(
				trainingPlanRepository, athleteId, name, startDate, endDate, null);
		if (defaultTrainingEnvironmentId != null) {
			TrainingEnvironmentSupport.requireOwnedActive(
					trainingEnvironmentRepository,
					athleteId,
					TrainingEnvironmentId.of(defaultTrainingEnvironmentId));
		}

		try {
			TrainingPlan plan = TrainingPlan.create(
					TrainingPlanId.generate(),
					athleteId,
					type,
					customTypeName,
					name,
					description,
					startDate,
					endDate,
					athleteSportId == null ? null : AthleteSportId.of(athleteSportId),
					athleteGoalId == null ? null : AthleteGoalId.of(athleteGoalId),
					clock);
			if (defaultTrainingEnvironmentId != null) {
				plan.linkDefaultTrainingEnvironment(
						TrainingEnvironmentId.of(defaultTrainingEnvironmentId), clock);
			}
			return TrainingPlanSupport.toResult(trainingPlanRepository.save(plan));
		}
		catch (IllegalArgumentException ex) {
			throw TrainingPlanSupport.translateValidation(ex);
		}
	}

}
