package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.util.UUID;

public record PlacementResponse(
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
		String surface) {
}
