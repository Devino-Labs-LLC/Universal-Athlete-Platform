package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.util.UUID;

import com.devinolabs.uap.sponsorship.application.PlacementAudience;
import com.devinolabs.uap.sponsorship.application.PlacementSurface;

public record PlacementWriteRequest(
		PlacementSurface surface,
		PlacementAudience audience,
		UUID organizationId,
		UUID teamId) {
}
