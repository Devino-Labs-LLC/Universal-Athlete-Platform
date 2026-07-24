package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;

/**
 * PATCH command: {@code null} fields mean omitted (unchanged);
 * non-null wrappers carry an explicit value that may itself be {@code null} to clear.
 */
public record UpdateAthleteGoalCommand(
		String title,
		boolean titlePresent,
		String description,
		boolean descriptionPresent,
		GoalPriority priority,
		boolean priorityPresent,
		BigDecimal targetValue,
		boolean targetValuePresent,
		GoalTargetUnit targetUnit,
		boolean targetUnitPresent,
		String customTargetUnit,
		boolean customTargetUnitPresent,
		LocalDate targetDate,
		boolean targetDatePresent,
		UUID athleteSportId,
		boolean athleteSportIdPresent) {
}
