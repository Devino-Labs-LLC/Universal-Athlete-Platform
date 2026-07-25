package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.training.domain.WorkoutExerciseStatusAction;

public record WorkoutExerciseStatusRequest(@NotNull WorkoutExerciseStatusAction action) {
}
