package com.devinolabs.uap.marketplace.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;

class ListingWindowTest {

	private static final Instant NOW = Instant.parse("2026-10-12T16:00:00Z");

	@Test
	void storesScheduledUntilStartAndEndsAfterWindow() {
		assertThat(ListingWindow.storedStatus(
				ListingStatus.ACTIVE, NOW.plusSeconds(3600), null, NOW)).isEqualTo(ListingStatus.SCHEDULED);
		assertThat(ListingWindow.storedStatus(
				ListingStatus.ACTIVE, NOW.minusSeconds(3600), NOW.minusSeconds(1), NOW))
				.isEqualTo(ListingStatus.ENDED);
		assertThat(ListingWindow.storedStatus(
				ListingStatus.ACTIVE, NOW.minusSeconds(10), NOW.plusSeconds(10), NOW))
				.isEqualTo(ListingStatus.ACTIVE);
	}

	@Test
	void presentableUsesServerClock() {
		MarketplaceListingEntity active = listing(ListingStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(60),
				null, null, "SAVE10");
		assertThat(ListingWindow.presentable(active, NOW)).isTrue();
		assertThat(ListingWindow.presentable(active, NOW.plusSeconds(120))).isFalse();
		assertThat(ListingWindow.offerLive(active, NOW)).isTrue();

		MarketplaceListingEntity paused = listing(ListingStatus.PAUSED, NOW.minusSeconds(60), null, null, null, "SAVE10");
		assertThat(ListingWindow.presentable(paused, NOW)).isFalse();
		assertThat(ListingWindow.offerLive(paused, NOW)).isFalse();

		MarketplaceListingEntity expiredOffer = listing(
				ListingStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(3600), NOW.minusSeconds(120), NOW.minusSeconds(1), "SAVE10");
		assertThat(ListingWindow.presentable(expiredOffer, NOW)).isTrue();
		assertThat(ListingWindow.offerLive(expiredOffer, NOW)).isFalse();
	}

	private static MarketplaceListingEntity listing(
			ListingStatus status,
			Instant starts,
			Instant ends,
			Instant offerStarts,
			Instant offerEnds,
			String promo) {
		return MarketplaceListingEntity.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"Bands",
				"Resistance bands",
				"Training equipment for accessory work.",
				"PRODUCT",
				"EQUIPMENT",
				status.name(),
				null,
				"https://shop.example/bands",
				"$29",
				"10% off",
				promo,
				false,
				"ALL",
				null,
				null,
				starts,
				ends,
				offerStarts,
				offerEnds,
				NOW);
	}
}
