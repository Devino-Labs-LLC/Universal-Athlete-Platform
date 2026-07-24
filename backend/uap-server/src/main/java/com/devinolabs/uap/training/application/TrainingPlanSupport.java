package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;

final class TrainingPlanSupport {

	private TrainingPlanSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static void assertLinks(
			AthleteContextPort athleteContextPort,
			AthleteRef athlete,
			UUID sportId,
			UUID goalId) {
		athleteContextPort.assertOptionalSportOwned(athlete.athleteId(), sportId);
		athleteContextPort.assertOptionalGoalOwned(athlete.athleteId(), goalId);
	}

	static void assertNoDuplicate(
			TrainingPlanRepository repository,
			AthleteId athleteId,
			String name,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId excludingId) {
		String normalizedName = TrainingPlan.normalizeName(name);
		if (repository.existsOverlappingDuplicate(athleteId, normalizedName, startDate, endDate, excludingId)) {
			throw new DuplicateTrainingPlanException();
		}
	}

	static TrainingPlanResult toResult(TrainingPlan plan) {
		return new TrainingPlanResult(
				plan.id(),
				plan.type(),
				plan.customTypeName(),
				plan.name(),
				plan.description(),
				plan.status(),
				plan.startDate(),
				plan.endDate(),
				plan.athleteSportId(),
				plan.athleteGoalId(),
				plan.createdAt(),
				plan.updatedAt());
	}

	static List<TrainingPlanResult> toResults(List<TrainingPlan> plans) {
		return plans.stream().map(TrainingPlanSupport::toResult).toList();
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("customTypeName")) {
			return new InvalidCustomTrainingPlanTypeException(message);
		}
		if (message.contains("startDate") || message.contains("endDate")) {
			return new InvalidTrainingPlanDatesException(message);
		}
		return ex;
	}

}
