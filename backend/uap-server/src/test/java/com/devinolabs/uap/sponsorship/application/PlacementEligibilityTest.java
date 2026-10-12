package com.devinolabs.uap.sponsorship.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipEventRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementRepository;

@ExtendWith(MockitoExtension.class)
class PlacementEligibilityTest {

	private static final Instant NOW = Instant.parse("2026-10-12T16:00:00Z");

	@Mock
	private OrganizationMembershipPort membershipPort;
	@Mock
	private SponsorRepository sponsorRepository;
	@Mock
	private SponsorshipCampaignRepository campaignRepository;
	@Mock
	private SponsorshipPlacementRepository placementRepository;
	@Mock
	private SponsorshipEventRepository eventRepository;

	@Test
	void organizationAudienceRequiresMembershipAndIgnoresOthers() {
		UUID account = UUID.randomUUID();
		UUID org = UUID.randomUUID();
		SponsorEntity sponsor = SponsorEntity.create(
				UUID.randomUUID(), "Acme", "Acme", "ACTIVE", null, null, null, NOW);
		SponsorshipCampaignEntity campaign = SponsorshipCampaignEntity.create(
				UUID.randomUUID(),
				sponsor.getId(),
				"Fall",
				"ACTIVE",
				NOW.minusSeconds(10),
				null,
				"Hi",
				"Body",
				"Go",
				"https://acme.example",
				null,
				NOW);
		SponsorshipPlacementEntity orgPlacement = SponsorshipPlacementEntity.create(
				UUID.randomUUID(), campaign.getId(), "HOME", "ORGANIZATION", org, null, NOW);
		when(placementRepository.findAllBySurface("HOME")).thenReturn(List.of(orgPlacement));
		when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));
		when(sponsorRepository.findById(sponsor.getId())).thenReturn(Optional.of(sponsor));
		when(membershipPort.hasActiveOrganizationMembership(account, org)).thenReturn(false);

		PlacementService service = new PlacementService(
				membershipPort,
				sponsorRepository,
				campaignRepository,
				placementRepository,
				eventRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(service.resolve(account, PlacementSurface.HOME, null)).isEmpty();

		when(membershipPort.hasActiveOrganizationMembership(account, org)).thenReturn(true);
		assertThat(service.resolve(account, PlacementSurface.HOME, null)).isPresent();
	}
}
