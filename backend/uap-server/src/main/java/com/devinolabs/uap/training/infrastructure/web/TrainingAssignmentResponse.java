package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TrainingAssignment;

record TrainingAssignmentResponse(
		UUID id,
		UUID teamId,
		UUID athleteId,
		String title,
		String description,
		LocalDate scheduledDate,
		String status,
		String athleteResponseNote,
		Instant respondedAt,
		String provenance,
		String assignedByRole,
		long version) {

	static TrainingAssignmentResponse from(TrainingAssignment assignment) {
		return new TrainingAssignmentResponse(
				assignment.id(),
				assignment.teamId(),
				assignment.athleteId(),
				assignment.title(),
				assignment.description(),
				assignment.scheduledDate(),
				assignment.status().name(),
				assignment.athleteResponseNote(),
				assignment.respondedAt(),
				"COACH_ASSIGNMENT",
				assignment.assignedByRole(),
				assignment.version());
	}

	static List<TrainingAssignmentResponse> fromList(List<TrainingAssignment> assignments) {
		return assignments.stream().map(TrainingAssignmentResponse::from).toList();
	}

}
