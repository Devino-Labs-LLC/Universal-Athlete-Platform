package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;

/**
 * PATCH body. Omitted properties deserialize as {@code null} {@link PatchValue} references
 * (unchanged). Explicit JSON null or a value becomes a present {@link PatchValue}.
 */
public record UpdateAthleteGoalRequest(
		PatchValue<String> title,
		PatchValue<String> description,
		PatchValue<GoalPriority> priority,
		PatchValue<BigDecimal> targetValue,
		PatchValue<GoalTargetUnit> targetUnit,
		PatchValue<String> customTargetUnit,
		PatchValue<LocalDate> targetDate,
		PatchValue<UUID> athleteSportId) {
}
