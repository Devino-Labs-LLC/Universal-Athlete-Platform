package com.devinolabs.uap.marketplace.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.marketplace.application.ListingAudience;
import com.devinolabs.uap.marketplace.application.ListingStatus;
import com.devinolabs.uap.marketplace.application.ListingType;

public record ListingWriteRequest(
		UUID partnerId,
		String title,
		String shortDescription,
		String fullDescription,
		ListingType listingType,
		String categoryCode,
		ListingStatus status,
		Boolean featured,
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
		Instant offerEndsAt) {
}
