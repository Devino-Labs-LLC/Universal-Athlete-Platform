package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;

public record TrainingPlanStatusRequest(@NotNull TrainingPlanStatusAction action) {
}
