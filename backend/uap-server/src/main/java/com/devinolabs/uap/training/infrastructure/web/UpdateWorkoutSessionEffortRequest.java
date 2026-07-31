package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutSessionEffortRequest(
		@NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal sessionRpe,
		@Min(1) @Max(1440) Integer sessionDurationMinutes,
		@Size(max = 1000) String perceivedNotes) {
}
