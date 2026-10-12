package com.devinolabs.uap.sponsorship.application;

import java.time.Instant;
import java.util.UUID;

public record SponsorRecord(
		UUID id,
		String name,
		String displayName,
		SponsorStatus status,
		String logoUrl,
		String websiteUrl,
		String description,
		Instant createdAt,
		Instant updatedAt) {
}
