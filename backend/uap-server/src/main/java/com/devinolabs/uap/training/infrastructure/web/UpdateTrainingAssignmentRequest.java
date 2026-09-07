package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;

record UpdateTrainingAssignmentRequest(
		long expectedVersion,
		String title,
		String description,
		LocalDate scheduledDate) {
}
