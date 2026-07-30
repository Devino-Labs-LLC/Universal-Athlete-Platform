package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SetWorkoutOccurrenceEnvironmentRequest(@NotNull UUID trainingEnvironmentId) {
}
