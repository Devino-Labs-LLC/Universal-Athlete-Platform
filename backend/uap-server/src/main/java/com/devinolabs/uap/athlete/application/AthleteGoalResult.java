package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;

public record AthleteGoalResult(
		AthleteGoalId id,
		GoalType goalType,
		String customGoalName,
		String title,
		String description,
		GoalPriority priority,
		GoalStatus status,
		BigDecimal targetValue,
		GoalTargetUnit targetUnit,
		String customTargetUnit,
		LocalDate targetDate,
		AthleteSportId athleteSportId,
		Instant createdAt,
		Instant updatedAt,
		Instant completedAt) {
}
