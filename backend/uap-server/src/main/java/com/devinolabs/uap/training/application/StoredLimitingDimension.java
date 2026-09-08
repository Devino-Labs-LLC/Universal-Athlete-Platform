package com.devinolabs.uap.training.application;

import java.util.UUID;

import com.devinolabs.uap.training.domain.ReadinessDimensionType;

public record StoredLimitingDimension(UUID athleteId, ReadinessDimensionType dimensionType) {
}
