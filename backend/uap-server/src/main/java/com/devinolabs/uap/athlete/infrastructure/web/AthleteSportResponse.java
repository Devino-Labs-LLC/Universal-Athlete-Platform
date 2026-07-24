package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;

public record AthleteSportResponse(
		String id,
		String sportType,
		String customSportName,
		boolean primarySport,
		String participationLevel,
		String preferredPosition,
		int yearsExperience,
		String seasonStatus,
		Instant createdAt,
		Instant updatedAt) {
}
