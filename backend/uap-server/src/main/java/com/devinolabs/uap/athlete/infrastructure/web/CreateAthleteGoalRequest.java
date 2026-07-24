package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;

public record CreateAthleteGoalRequest(
		@NotNull GoalType goalType,
		@Size(max = 120) String customGoalName,
		@NotBlank @Size(max = 160) String title,
		@Size(max = 1000) String description,
		GoalPriority priority,
		@DecimalMin(value = "0.001", inclusive = true) BigDecimal targetValue,
		GoalTargetUnit targetUnit,
		@Size(max = 60) String customTargetUnit,
		LocalDate targetDate,
		UUID athleteSportId) {
}
