package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record UpdateAthleteMeasurementCommand(
		BigDecimal value,
		boolean valuePresent,
		MeasurementUnit unit,
		boolean unitPresent,
		String customUnit,
		boolean customUnitPresent,
		String notes,
		boolean notesPresent,
		Instant measuredAt,
		boolean measuredAtPresent,
		UUID athleteSportId,
		boolean athleteSportIdPresent,
		UUID athleteGoalId,
		boolean athleteGoalIdPresent) {
}
