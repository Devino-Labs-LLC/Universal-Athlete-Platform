package com.devinolabs.uap.athlete.infrastructure.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;

public record UpdateAthleteSportRequest(
		@NotNull ParticipationLevel participationLevel,
		@Size(max = 100) String preferredPosition,
		@Min(0) @Max(80) int yearsExperience,
		@NotNull SeasonStatus seasonStatus) {
}
