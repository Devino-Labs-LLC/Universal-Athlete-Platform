package com.devinolabs.uap.training.application;

import java.util.UUID;

import com.devinolabs.uap.training.domain.ReadinessBand;

public record CurrentReadinessSlice(
		UUID athleteId,
		UUID assessmentId,
		ReadinessBand readinessBand) {
}
