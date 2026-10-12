package com.devinolabs.uap.sponsorship.application;

import java.util.UUID;

public record PlacementRecord(
		UUID id,
		UUID campaignId,
		PlacementSurface surface,
		PlacementAudience audience,
		UUID organizationId,
		UUID teamId) {
}
