package com.devinolabs.uap.marketplace.application;

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

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceCategoryRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceEventRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceSaveRepository;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort.PartnerRef;

@ExtendWith(MockitoExtension.class)
class MarketplaceEligibilityTest {

	private static final Instant NOW = Instant.parse("2026-10-12T16:00:00Z");

	@Mock
	private OrganizationMembershipPort membershipPort;
	@Mock
	private SponsorPartnerPort partnerPort;
	@Mock
	private MarketplaceListingRepository listingRepository;
	@Mock
	private MarketplaceCategoryRepository categoryRepository;
	@Mock
	private MarketplaceSaveRepository saveRepository;
	@Mock
	private MarketplaceEventRepository eventRepository;

	@Test
	void organizationAudienceRequiresMembership() {
		UUID account = UUID.randomUUID();
		UUID org = UUID.randomUUID();
		UUID partnerId = UUID.randomUUID();
		MarketplaceListingEntity listing = MarketplaceListingEntity.create(
				UUID.randomUUID(),
				partnerId,
				"Team kit",
				"Apparel for the roster",
				"Team-only apparel.",
				"PRODUCT",
				"APPAREL",
				"ACTIVE",
				null,
				"https://shop.example/kit",
				null,
				null,
				null,
				false,
				"ORGANIZATION",
				org,
				null,
				NOW.minusSeconds(10),
				null,
				null,
				null,
				NOW);
		when(listingRepository.findAll()).thenReturn(List.of(listing));
		when(partnerPort.findPartner(partnerId))
				.thenReturn(Optional.of(new PartnerRef(partnerId, "Acme", "ACTIVE", null)));
		when(membershipPort.hasActiveOrganizationMembership(account, org)).thenReturn(false);
		when(saveRepository.findAllByAccountIdOrderByCreatedAtDesc(account)).thenReturn(List.of());
		when(categoryRepository.findById("APPAREL")).thenReturn(Optional.empty());

		MarketplaceDiscoveryService service = service();
		assertThat(service.browse(account, null, null, null, null, 0, 20).listings()).isEmpty();

		when(membershipPort.hasActiveOrganizationMembership(account, org)).thenReturn(true);
		assertThat(service.browse(account, null, null, null, null, 0, 20).listings()).hasSize(1);
	}

	private MarketplaceDiscoveryService service() {
		return new MarketplaceDiscoveryService(
				membershipPort,
				partnerPort,
				listingRepository,
				categoryRepository,
				saveRepository,
				eventRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}
}
