package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTrainingPlanCommand(
		String name,
		boolean namePresent,
		String description,
		boolean descriptionPresent,
		LocalDate startDate,
		boolean startDatePresent,
		LocalDate endDate,
		boolean endDatePresent,
		UUID athleteSportId,
		boolean athleteSportIdPresent,
		UUID athleteGoalId,
		boolean athleteGoalIdPresent) {
}
