package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

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

@Service
public class UpdateTrainingPlanUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final Clock clock;

	public UpdateTrainingPlanUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingPlanResult execute(AccountId accountId, TrainingPlanId planId, UpdateTrainingPlanCommand command) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = trainingPlanRepository.findByIdAndAthleteId(planId, athleteId)
				.orElseThrow(TrainingPlanNotFoundException::new);

		if (command.athleteSportIdPresent()) {
			athleteContextPort.assertOptionalSportOwned(athlete.athleteId(), command.athleteSportId());
		}
		if (command.athleteGoalIdPresent()) {
			athleteContextPort.assertOptionalGoalOwned(athlete.athleteId(), command.athleteGoalId());
		}

		String nameForDuplicate = command.namePresent() ? command.name() : plan.name();
		LocalDate startForDuplicate = command.startDatePresent() ? command.startDate() : plan.startDate();
		LocalDate endForDuplicate = command.endDatePresent() ? command.endDate() : plan.endDate();
		if (plan.isDuplicateCandidate()) {
			TrainingPlanSupport.assertNoDuplicate(
					trainingPlanRepository,
					athleteId,
					nameForDuplicate,
					startForDuplicate,
					endForDuplicate,
					plan.id());
		}

		try {
			if (command.namePresent()) {
				if (command.name() == null || command.name().isBlank()) {
					throw new IllegalArgumentException("name must not be blank");
				}
				plan.rename(command.name(), clock);
			}
			if (command.descriptionPresent()) {
				plan.changeDescription(command.description(), clock);
			}
			if (command.startDatePresent() || command.endDatePresent()) {
				if (command.startDatePresent() && command.startDate() == null) {
					throw new IllegalArgumentException("startDate must not be null");
				}
				if (command.endDatePresent() && command.endDate() == null) {
					throw new IllegalArgumentException("endDate must not be null");
				}
				plan.changeDates(
						command.startDatePresent() ? command.startDate() : null,
						command.endDatePresent() ? command.endDate() : null,
						clock);
			}
			if (command.athleteSportIdPresent()) {
				if (command.athleteSportId() == null) {
					plan.unlinkSport(clock);
				}
				else {
					plan.linkSport(AthleteSportId.of(command.athleteSportId()), clock);
				}
			}
			if (command.athleteGoalIdPresent()) {
				if (command.athleteGoalId() == null) {
					plan.unlinkGoal(clock);
				}
				else {
					plan.linkGoal(AthleteGoalId.of(command.athleteGoalId()), clock);
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw TrainingPlanSupport.translateValidation(ex);
		}

		return TrainingPlanSupport.toResult(trainingPlanRepository.save(plan));
	}

}
