package com.devinolabs.uap.marketplace.application;

import java.util.UUID;

public record MarketplaceReportRow(
		UUID listingId,
		String title,
		UUID partnerId,
		String partnerName,
		String categoryCode,
		ListingType listingType,
		long impressions,
		long detailViews,
		long outboundClicks,
		long redemptionInteractions,
		long saves,
		double clickThroughRate) {
}
