package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;

public record AthleteGoalResponse(
		UUID id,
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
		UUID athleteSportId,
		Instant createdAt,
		Instant updatedAt,
		Instant completedAt) {
}
