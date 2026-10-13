package com.devinolabs.uap.marketplace.application;

import java.time.Instant;
import java.util.UUID;

public record OperatorListingRecord(
		UUID id,
		UUID partnerId,
		String title,
		String shortDescription,
		String fullDescription,
		ListingType listingType,
		String categoryCode,
		ListingStatus status,
		boolean featured,
		ListingAudience audience,
		UUID organizationId,
		UUID teamId,
		String imageUrl,
		String destinationUrl,
		String priceDisplay,
		String discountText,
		String promoCode,
		Instant startsAt,
		Instant endsAt,
		Instant offerStartsAt,
		Instant offerEndsAt,
		Instant createdAt,
		Instant updatedAt) {
}
