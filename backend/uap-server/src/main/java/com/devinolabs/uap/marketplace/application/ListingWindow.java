package com.devinolabs.uap.marketplace.application;

import java.time.Instant;

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;

public final class ListingWindow {

	private ListingWindow() {
	}

	public static boolean presentable(MarketplaceListingEntity listing, Instant now) {
		ListingStatus status = ListingStatus.valueOf(listing.getStatus());
		if (status != ListingStatus.ACTIVE && status != ListingStatus.SCHEDULED) {
			return false;
		}
		if (now.isBefore(listing.getStartsAt())) {
			return false;
		}
		return listing.getEndsAt() == null || now.isBefore(listing.getEndsAt());
	}

	public static boolean offerLive(MarketplaceListingEntity listing, Instant now) {
		if (!presentable(listing, now)) {
			return false;
		}
		if (listing.getPromoCode() == null
				&& listing.getDiscountText() == null
				&& listing.getOfferStartsAt() == null
				&& listing.getOfferEndsAt() == null) {
			return false;
		}
		if (listing.getOfferStartsAt() != null && now.isBefore(listing.getOfferStartsAt())) {
			return false;
		}
		return listing.getOfferEndsAt() == null || now.isBefore(listing.getOfferEndsAt());
	}

	public static ListingStatus storedStatus(ListingStatus requested, Instant startsAt, Instant endsAt, Instant now) {
		if (requested == ListingStatus.ACTIVE || requested == ListingStatus.SCHEDULED) {
			if (endsAt != null && !now.isBefore(endsAt)) {
				return ListingStatus.ENDED;
			}
			if (now.isBefore(startsAt)) {
				return ListingStatus.SCHEDULED;
			}
			return ListingStatus.ACTIVE;
		}
		return requested;
	}
}
