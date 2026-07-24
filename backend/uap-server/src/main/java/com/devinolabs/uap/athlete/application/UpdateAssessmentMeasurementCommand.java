package com.devinolabs.uap.athlete.application;

public record UpdateAssessmentMeasurementCommand(
		Integer displayOrder,
		boolean displayOrderPresent,
		String label,
		boolean labelPresent,
		String notes,
		boolean notesPresent) {
}
