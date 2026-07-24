package com.devinolabs.uap.athlete.infrastructure.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReorderAssessmentMeasurementsRequest(
		@NotEmpty List<@NotNull UUID> attachmentIds) {
}
