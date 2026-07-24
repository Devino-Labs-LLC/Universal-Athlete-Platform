package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

public record AssessmentMeasurementResult(
		AssessmentMeasurementId id,
		AthleteMeasurementId measurementId,
		int displayOrder,
		String label,
		String notes,
		boolean snapshotted,
		MeasurementType measurementType,
		String customMeasurementName,
		BigDecimal value,
		MeasurementUnit unit,
		String customUnit,
		MeasurementSource source,
		Instant measuredAt,
		UUID athleteSportId,
		UUID athleteGoalId,
		Instant snapshottedAt,
		Instant createdAt,
		Instant updatedAt) {
}
