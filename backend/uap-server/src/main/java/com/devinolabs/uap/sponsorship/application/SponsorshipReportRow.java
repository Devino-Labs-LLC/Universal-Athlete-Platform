package com.devinolabs.uap.sponsorship.application;

import java.util.UUID;

public record SponsorshipReportRow(
		UUID sponsorId,
		String sponsorName,
		UUID campaignId,
		String campaignName,
		PlacementSurface surface,
		long impressions,
		long clicks,
		double clickThroughRate) {
}
