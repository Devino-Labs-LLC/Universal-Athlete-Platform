package com.devinolabs.uap.sponsorship.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipEventEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipEventRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementRepository;

@Service
public class PlacementService {

	private final OrganizationMembershipPort membershipPort;
	private final SponsorRepository sponsorRepository;
	private final SponsorshipCampaignRepository campaignRepository;
	private final SponsorshipPlacementRepository placementRepository;
	private final SponsorshipEventRepository eventRepository;
	private final Clock clock;

	public PlacementService(
			OrganizationMembershipPort membershipPort,
			SponsorRepository sponsorRepository,
			SponsorshipCampaignRepository campaignRepository,
			SponsorshipPlacementRepository placementRepository,
			SponsorshipEventRepository eventRepository,
			Clock clock) {
		this.membershipPort = membershipPort;
		this.sponsorRepository = sponsorRepository;
		this.campaignRepository = campaignRepository;
		this.placementRepository = placementRepository;
		this.eventRepository = eventRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<ResolvedPlacement> resolve(UUID accountId, PlacementSurface surface, UUID teamId) {
		Instant now = clock.instant();
		return placementRepository.findAllBySurface(surface.name()).stream()
				.filter(placement -> eligible(accountId, placement, surface, teamId))
				.map(placement -> candidate(placement, now))
				.flatMap(Optional::stream)
				.max(Comparator.comparing(ResolvedPlacement::placementId));
	}

	@Transactional
	public void record(UUID accountId, UUID placementId, SponsorshipEventKind kind, UUID teamId) {
		SponsorshipPlacementEntity placement = placementRepository.findById(placementId)
				.orElseThrow(SponsorCatalogNotFoundException::new);
		Instant now = clock.instant();
		Optional<ResolvedPlacement> eligible = candidate(placement, now)
				.filter(resolved -> eligible(
						accountId,
						placement,
						PlacementSurface.valueOf(placement.getSurface()),
						teamId));
		if (eligible.isEmpty()) {
			throw new SponsorCatalogNotFoundException();
		}
		Instant bucket = now.truncatedTo(ChronoUnit.HOURS);
		if (eventRepository
				.findByAccountIdAndCampaignIdAndSurfaceAndKindAndBucket(
						accountId, placement.getCampaignId(), placement.getSurface(), kind.name(), bucket)
				.isPresent()) {
			return;
		}
		try {
			eventRepository.save(SponsorshipEventEntity.create(
					UUID.randomUUID(),
					placement.getCampaignId(),
					placement.getId(),
					placement.getSurface(),
					kind.name(),
					accountId,
					bucket,
					now));
		}
		catch (DataIntegrityViolationException ignored) {
			// Concurrent duplicate in the same hour bucket.
		}
	}

	private Optional<ResolvedPlacement> candidate(SponsorshipPlacementEntity placement, Instant now) {
		Optional<SponsorshipCampaignEntity> campaign = campaignRepository.findById(placement.getCampaignId());
		if (campaign.isEmpty() || !CampaignWindow.presentable(campaign.get(), now)) {
			return Optional.empty();
		}
		Optional<SponsorEntity> sponsor = sponsorRepository.findById(campaign.get().getSponsorId());
		if (sponsor.isEmpty() || SponsorStatus.valueOf(sponsor.get().getStatus()) != SponsorStatus.ACTIVE) {
			return Optional.empty();
		}
		SponsorshipCampaignEntity active = campaign.get();
		SponsorEntity brand = sponsor.get();
		return Optional.of(new ResolvedPlacement(
				placement.getId(),
				active.getId(),
				brand.getId(),
				brand.getDisplayName(),
				"Sponsored",
				active.getHeadline(),
				active.getBody(),
				active.getCtaLabel(),
				active.getDestinationUrl(),
				active.getCreativeUrl(),
				PlacementSurface.valueOf(placement.getSurface())));
	}

	private boolean eligible(
			UUID accountId,
			SponsorshipPlacementEntity placement,
			PlacementSurface surface,
			UUID teamId) {
		if (surface == PlacementSurface.TEAM && teamId == null) {
			return PlacementAudience.valueOf(placement.getAudience()) == PlacementAudience.ALL;
		}
		if (surface == PlacementSurface.TEAM && !membershipPort.canViewTeam(accountId, teamId)) {
			return false;
		}
		return switch (PlacementAudience.valueOf(placement.getAudience())) {
			case ALL -> true;
			case ORGANIZATION -> placement.getOrganizationId() != null
					&& membershipPort.hasActiveOrganizationMembership(accountId, placement.getOrganizationId());
			case TEAM -> placement.getTeamId() != null
					&& membershipPort.findActiveTeamMembership(accountId, placement.getTeamId()).isPresent()
					&& (teamId == null || teamId.equals(placement.getTeamId()));
		};
	}
}
