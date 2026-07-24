package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record UpdateAthleteMeasurementRequest(
		PatchValue<BigDecimal> value,
		PatchValue<MeasurementUnit> unit,
		PatchValue<String> customUnit,
		PatchValue<String> notes,
		PatchValue<Instant> measuredAt,
		PatchValue<UUID> athleteSportId,
		PatchValue<UUID> athleteGoalId) {
}
