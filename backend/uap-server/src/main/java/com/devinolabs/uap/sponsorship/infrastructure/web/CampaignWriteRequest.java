package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.sponsorship.application.CampaignStatus;

public record CampaignWriteRequest(
		UUID sponsorId,
		String name,
		CampaignStatus status,
		Instant startsAt,
		Instant endsAt,
		String headline,
		String body,
		String ctaLabel,
		String destinationUrl,
		String creativeUrl) {
}
