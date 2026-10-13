package com.devinolabs.uap.marketplace.application;

import java.util.UUID;

public record ListingRecord(
		UUID id,
		UUID partnerId,
		String partnerDisplayName,
		String title,
		String shortDescription,
		String fullDescription,
		ListingType listingType,
		String categoryCode,
		String categoryName,
		boolean featured,
		boolean available,
		boolean offerActive,
		String imageUrl,
		String destinationUrl,
		String priceDisplay,
		String discountText,
		String promoCode,
		boolean saved) {
}
