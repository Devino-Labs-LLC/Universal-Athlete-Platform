package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record RecordAthleteMeasurementRequest(
		@NotNull MeasurementType measurementType,
		@Size(max = 120) String customMeasurementName,
		@NotNull BigDecimal value,
		@NotNull MeasurementUnit unit,
		@Size(max = 60) String customUnit,
		MeasurementSource source,
		@Size(max = 1000) String notes,
		@NotNull Instant measuredAt,
		UUID athleteSportId,
		UUID athleteGoalId) {
}
