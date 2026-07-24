package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;

@Service
public class ChangeTrainingPlanStatusUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final Clock clock;

	public ChangeTrainingPlanStatusUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingPlanResult execute(AccountId accountId, TrainingPlanId planId, TrainingPlanStatusAction action) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = trainingPlanRepository.findByIdAndAthleteId(planId, athleteId)
				.orElseThrow(TrainingPlanNotFoundException::new);

		try {
			plan.applyStatusAction(action, clock);
		}
		catch (IllegalStateException ex) {
			throw new InvalidTrainingPlanStatusException(ex.getMessage());
		}

		if (plan.isDuplicateCandidate()) {
			TrainingPlanSupport.assertNoDuplicate(
					trainingPlanRepository,
					athleteId,
					plan.name(),
					plan.startDate(),
					plan.endDate(),
					plan.id());
		}

		return TrainingPlanSupport.toResult(trainingPlanRepository.save(plan));
	}

}
