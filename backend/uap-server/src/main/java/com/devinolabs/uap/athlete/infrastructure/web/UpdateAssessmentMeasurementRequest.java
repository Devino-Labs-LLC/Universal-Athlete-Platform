package com.devinolabs.uap.athlete.infrastructure.web;

public record UpdateAssessmentMeasurementRequest(
		PatchValue<Integer> displayOrder,
		PatchValue<String> label,
		PatchValue<String> notes) {
}
