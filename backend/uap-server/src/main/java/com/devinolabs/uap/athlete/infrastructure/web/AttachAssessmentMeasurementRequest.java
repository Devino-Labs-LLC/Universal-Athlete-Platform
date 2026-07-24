package com.devinolabs.uap.athlete.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttachAssessmentMeasurementRequest(
		@NotNull UUID measurementId,
		@Min(0) Integer displayOrder,
		@Size(max = 160) String label,
		@Size(max = 1000) String notes) {
}
