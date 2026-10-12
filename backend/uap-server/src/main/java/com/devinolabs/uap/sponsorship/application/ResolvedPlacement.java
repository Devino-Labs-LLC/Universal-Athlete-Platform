package com.devinolabs.uap.sponsorship.application;

import java.util.UUID;

public record ResolvedPlacement(
		UUID placementId,
		UUID campaignId,
		UUID sponsorId,
		String sponsorDisplayName,
		String label,
		String headline,
		String body,
		String ctaLabel,
		String destinationUrl,
		String creativeUrl,
		PlacementSurface surface) {
}
