package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;

record CreateTrainingAssignmentRequest(
		String title,
		String description,
		LocalDate scheduledDate,
		String idempotencyKey) {
}
