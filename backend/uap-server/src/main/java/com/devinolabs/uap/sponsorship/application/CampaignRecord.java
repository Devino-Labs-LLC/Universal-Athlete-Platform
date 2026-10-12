package com.devinolabs.uap.sponsorship.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignRecord(
		UUID id,
		UUID sponsorId,
		String name,
		CampaignStatus status,
		Instant startsAt,
		Instant endsAt,
		String headline,
		String body,
		String ctaLabel,
		String destinationUrl,
		String creativeUrl,
		Instant createdAt,
		Instant updatedAt,
		List<PlacementRecord> placements) {
}
