package com.devinolabs.uap.sponsorship.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;

class CampaignWindowTest {

	private static final Instant NOW = Instant.parse("2026-10-12T16:00:00Z");

	@Test
	void storesScheduledUntilStartAndEndsAfterWindow() {
		assertThat(CampaignWindow.storedStatus(
				CampaignStatus.ACTIVE,
				NOW.plusSeconds(3600),
				null,
				NOW)).isEqualTo(CampaignStatus.SCHEDULED);
		assertThat(CampaignWindow.storedStatus(
				CampaignStatus.ACTIVE,
				NOW.minusSeconds(3600),
				NOW.minusSeconds(1),
				NOW)).isEqualTo(CampaignStatus.ENDED);
		assertThat(CampaignWindow.storedStatus(
				CampaignStatus.ACTIVE,
				NOW.minusSeconds(10),
				NOW.plusSeconds(10),
				NOW)).isEqualTo(CampaignStatus.ACTIVE);
	}

	@Test
	void presentableUsesServerClock() {
		SponsorshipCampaignEntity active = campaign(CampaignStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(60));
		assertThat(CampaignWindow.presentable(active, NOW)).isTrue();
		assertThat(CampaignWindow.presentable(active, NOW.plusSeconds(120))).isFalse();

		SponsorshipCampaignEntity paused = campaign(CampaignStatus.PAUSED, NOW.minusSeconds(60), null);
		assertThat(CampaignWindow.presentable(paused, NOW)).isFalse();

		SponsorshipCampaignEntity future = campaign(CampaignStatus.SCHEDULED, NOW.plusSeconds(60), null);
		assertThat(CampaignWindow.presentable(future, NOW)).isFalse();
		assertThat(CampaignWindow.presentable(future, NOW.plusSeconds(120))).isTrue();
	}

	private static SponsorshipCampaignEntity campaign(CampaignStatus status, Instant starts, Instant ends) {
		return SponsorshipCampaignEntity.create(
				java.util.UUID.randomUUID(),
				java.util.UUID.randomUUID(),
				"Spring",
				status.name(),
				starts,
				ends,
				"Headline",
				"Body",
				"Learn more",
				"https://partners.example.com",
				null,
				NOW);
	}
}
