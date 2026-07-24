package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record AthleteMeasurementResult(
		AthleteMeasurementId id,
		MeasurementType measurementType,
		String customMeasurementName,
		BigDecimal value,
		MeasurementUnit unit,
		String customUnit,
		MeasurementSource source,
		String notes,
		Instant measuredAt,
		AthleteSportId athleteSportId,
		AthleteGoalId athleteGoalId,
		Instant createdAt,
		Instant updatedAt) {
}
