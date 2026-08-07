package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record GenerateDailyReadinessAssessmentRequest(
		@NotNull UUID dailyAthleteStateSnapshotId) {
}
