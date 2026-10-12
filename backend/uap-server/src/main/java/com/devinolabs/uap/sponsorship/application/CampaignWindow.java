package com.devinolabs.uap.sponsorship.application;

import java.time.Instant;

import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;

public final class CampaignWindow {

	private CampaignWindow() {
	}

	public static boolean presentable(SponsorshipCampaignEntity campaign, Instant now) {
		CampaignStatus status = CampaignStatus.valueOf(campaign.getStatus());
		if (status != CampaignStatus.ACTIVE && status != CampaignStatus.SCHEDULED) {
			return false;
		}
		if (now.isBefore(campaign.getStartsAt())) {
			return false;
		}
		return campaign.getEndsAt() == null || now.isBefore(campaign.getEndsAt());
	}

	public static CampaignStatus storedStatus(CampaignStatus requested, Instant startsAt, Instant endsAt, Instant now) {
		if (requested == CampaignStatus.ACTIVE || requested == CampaignStatus.SCHEDULED) {
			if (endsAt != null && !now.isBefore(endsAt)) {
				return CampaignStatus.ENDED;
			}
			if (now.isBefore(startsAt)) {
				return CampaignStatus.SCHEDULED;
			}
			return CampaignStatus.ACTIVE;
		}
		return requested;
	}
}
