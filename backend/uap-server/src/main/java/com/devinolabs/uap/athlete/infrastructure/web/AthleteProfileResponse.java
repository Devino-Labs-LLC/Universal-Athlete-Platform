package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AthleteProfileResponse(
		String id,
		String firstName,
		String lastName,
		LocalDate dateOfBirth,
		String sex,
		BigDecimal heightCm,
		BigDecimal weightKg,
		String dominantHand,
		String dominantFoot,
		String status,
		Instant createdAt,
		Instant updatedAt) {
}
