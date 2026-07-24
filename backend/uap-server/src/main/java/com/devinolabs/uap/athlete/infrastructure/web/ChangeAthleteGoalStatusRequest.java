package com.devinolabs.uap.athlete.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.athlete.domain.GoalStatusAction;

public record ChangeAthleteGoalStatusRequest(@NotNull GoalStatusAction action) {
}
