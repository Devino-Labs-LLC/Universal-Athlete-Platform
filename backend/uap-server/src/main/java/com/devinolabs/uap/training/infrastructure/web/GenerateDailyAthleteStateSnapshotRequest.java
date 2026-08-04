package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.NotNull;

record GenerateDailyAthleteStateSnapshotRequest(
		@NotNull Integer baselineWindowDays) {
}
