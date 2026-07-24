package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.training.domain.WorkoutDayStatusAction;

public record WorkoutDayStatusRequest(@NotNull WorkoutDayStatusAction action) {
}
