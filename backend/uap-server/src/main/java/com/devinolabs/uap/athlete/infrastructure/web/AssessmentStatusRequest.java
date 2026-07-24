package com.devinolabs.uap.athlete.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.athlete.domain.AssessmentStatusAction;

public record AssessmentStatusRequest(@NotNull AssessmentStatusAction action) {
}
