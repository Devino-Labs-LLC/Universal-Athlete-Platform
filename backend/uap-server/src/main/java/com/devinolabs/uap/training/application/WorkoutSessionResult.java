package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutSessionId;
import com.devinolabs.uap.training.domain.WorkoutSessionStatus;

public record WorkoutSessionResult(
		WorkoutSessionId id,
		WorkoutSessionStatus status,
		Integer actualSets,
		Integer actualReps,
		BigDecimal actualWeight,
		WeightUnit weightUnit,
		Integer actualDurationSeconds,
		BigDecimal actualDistance,
		DistanceUnit distanceUnit,
		Integer actualRestSeconds,
		Integer actualRpe,
		Instant completedAt,
		String athleteNotes,
		Instant createdAt,
		Instant updatedAt) {
}
