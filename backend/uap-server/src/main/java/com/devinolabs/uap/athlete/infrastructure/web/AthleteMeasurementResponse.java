package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record AthleteMeasurementResponse(
		UUID id,
		MeasurementType measurementType,
		String customMeasurementName,
		BigDecimal value,
		MeasurementUnit unit,
		String customUnit,
		MeasurementSource source,
		String notes,
		Instant measuredAt,
		UUID athleteSportId,
		UUID athleteGoalId,
		Instant createdAt,
		Instant updatedAt) {
}
