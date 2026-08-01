package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDailyRecoveryCheckInRequest(
		@NotNull LocalDate checkInDate,
		@Min(0) @Max(1440) Integer sleepDurationMinutes,
		@Min(1) @Max(5) Integer sleepQuality,
		@NotNull @Min(1) @Max(5) Integer fatigue,
		@NotNull @Min(1) @Max(5) Integer muscleSoreness,
		@NotNull @Min(1) @Max(5) Integer stress,
		@NotNull @Min(1) @Max(5) Integer mood,
		@NotNull @Min(1) @Max(5) Integer motivation,
		List<BodyAreaDiscomfortRequest> discomfortAreas,
		@Size(max = 2000) String notes) {
}
