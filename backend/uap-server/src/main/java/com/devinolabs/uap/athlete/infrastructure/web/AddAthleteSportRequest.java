package com.devinolabs.uap.athlete.infrastructure.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.SportType;

public record AddAthleteSportRequest(
		@NotNull SportType sportType,
		@Size(max = 100) String customSportName,
		boolean primarySport,
		@NotNull ParticipationLevel participationLevel,
		@Size(max = 100) String preferredPosition,
		@Min(0) @Max(80) int yearsExperience,
		@NotNull SeasonStatus seasonStatus) {
}
