package com.devinolabs.uap.sponsorship.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipPlacementRepository;

@Service
public class SponsorshipCatalogService {

	private static final Map<SponsorStatus, Set<SponsorStatus>> SPONSOR_TRANSITIONS = Map.of(
			SponsorStatus.DRAFT, EnumSet.of(SponsorStatus.ACTIVE, SponsorStatus.ARCHIVED),
			SponsorStatus.ACTIVE, EnumSet.of(SponsorStatus.PAUSED, SponsorStatus.ARCHIVED),
			SponsorStatus.PAUSED, EnumSet.of(SponsorStatus.ACTIVE, SponsorStatus.ARCHIVED),
			SponsorStatus.ARCHIVED, EnumSet.noneOf(SponsorStatus.class));

	private static final Map<CampaignStatus, Set<CampaignStatus>> CAMPAIGN_TRANSITIONS = Map.of(
			CampaignStatus.DRAFT, EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE, CampaignStatus.ARCHIVED),
			CampaignStatus.SCHEDULED, EnumSet.of(
					CampaignStatus.DRAFT, CampaignStatus.ACTIVE, CampaignStatus.PAUSED, CampaignStatus.ARCHIVED),
			CampaignStatus.ACTIVE, EnumSet.of(CampaignStatus.PAUSED, CampaignStatus.ENDED, CampaignStatus.ARCHIVED),
			CampaignStatus.PAUSED, EnumSet.of(CampaignStatus.ACTIVE, CampaignStatus.ENDED, CampaignStatus.ARCHIVED),
			CampaignStatus.ENDED, EnumSet.of(CampaignStatus.ARCHIVED),
			CampaignStatus.ARCHIVED, EnumSet.noneOf(CampaignStatus.class));

	private final SponsorOperatorGuard operatorGuard;
	private final SponsorRepository sponsorRepository;
	private final SponsorshipCampaignRepository campaignRepository;
	private final SponsorshipPlacementRepository placementRepository;
	private final Clock clock;

	public SponsorshipCatalogService(
			SponsorOperatorGuard operatorGuard,
			SponsorRepository sponsorRepository,
			SponsorshipCampaignRepository campaignRepository,
			SponsorshipPlacementRepository placementRepository,
			Clock clock) {
		this.operatorGuard = operatorGuard;
		this.sponsorRepository = sponsorRepository;
		this.campaignRepository = campaignRepository;
		this.placementRepository = placementRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public boolean operator(UUID accountId) {
		return operatorGuard.isOperator(accountId);
	}

	@Transactional(readOnly = true)
	public List<SponsorRecord> listSponsors(UUID operatorId) {
		operatorGuard.requireOperator(operatorId);
		return sponsorRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toSponsor).toList();
	}

	@Transactional
	public SponsorRecord createSponsor(UUID operatorId, SponsorDraft draft) {
		operatorGuard.requireOperator(operatorId);
		Instant now = clock.instant();
		SponsorEntity entity = SponsorEntity.create(
				UUID.randomUUID(),
				SponsorCopy.requirePlain(draft.name(), "name", 120),
				SponsorCopy.requirePlain(draft.displayName(), "displayName", 120),
				SponsorStatus.DRAFT.name(),
				SponsorDestination.optionalHttps(draft.logoUrl(), "logoUrl"),
				SponsorDestination.optionalHttps(draft.websiteUrl(), "websiteUrl"),
				SponsorCopy.optionalPlain(draft.description(), "description", 500),
				now);
		return toSponsor(sponsorRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public SponsorRecord getSponsor(UUID operatorId, UUID sponsorId) {
		operatorGuard.requireOperator(operatorId);
		return toSponsor(sponsor(sponsorId));
	}

	@Transactional
	public SponsorRecord updateSponsor(UUID operatorId, UUID sponsorId, SponsorDraft draft) {
		operatorGuard.requireOperator(operatorId);
		SponsorEntity entity = sponsor(sponsorId);
		entity.update(
				SponsorCopy.requirePlain(draft.name(), "name", 120),
				SponsorCopy.requirePlain(draft.displayName(), "displayName", 120),
				SponsorDestination.optionalHttps(draft.logoUrl(), "logoUrl"),
				SponsorDestination.optionalHttps(draft.websiteUrl(), "websiteUrl"),
				SponsorCopy.optionalPlain(draft.description(), "description", 500),
				clock.instant());
		return toSponsor(sponsorRepository.save(entity));
	}

	@Transactional
	public SponsorRecord transitionSponsor(UUID operatorId, UUID sponsorId, SponsorStatus next) {
		operatorGuard.requireOperator(operatorId);
		SponsorEntity entity = sponsor(sponsorId);
		SponsorStatus current = SponsorStatus.valueOf(entity.getStatus());
		if (!SPONSOR_TRANSITIONS.get(current).contains(next)) {
			throw new SponsorInputInvalidException("Sponsor cannot move from " + current + " to " + next);
		}
		entity.setStatus(next.name(), clock.instant());
		return toSponsor(sponsorRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<CampaignRecord> listCampaigns(UUID operatorId) {
		operatorGuard.requireOperator(operatorId);
		return campaignRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toCampaign).toList();
	}

	@Transactional
	public CampaignRecord createCampaign(UUID operatorId, CampaignDraft draft) {
		operatorGuard.requireOperator(operatorId);
		SponsorEntity sponsor = sponsor(draft.sponsorId());
		Instant now = clock.instant();
		CampaignStatus stored = CampaignWindow.storedStatus(
				draft.status() == null ? CampaignStatus.DRAFT : draft.status(),
				draft.startsAt(),
				draft.endsAt(),
				now);
		if (draft.status() != null
				&& draft.status() != CampaignStatus.DRAFT
				&& !CAMPAIGN_TRANSITIONS.get(CampaignStatus.DRAFT).contains(draft.status())
				&& draft.status() != CampaignStatus.SCHEDULED
				&& draft.status() != CampaignStatus.ACTIVE) {
			throw new SponsorInputInvalidException("Invalid initial campaign status");
		}
		SponsorshipCampaignEntity entity = SponsorshipCampaignEntity.create(
				UUID.randomUUID(),
				sponsor.getId(),
				copy(draft),
				stored.name(),
				starts(draft.startsAt()),
				ends(draft.startsAt(), draft.endsAt()),
				SponsorCopy.requirePlain(draft.headline(), "headline", 160),
				SponsorCopy.requirePlain(draft.body(), "body", 500),
				SponsorCopy.requirePlain(draft.ctaLabel(), "ctaLabel", 40),
				SponsorDestination.requireHttps(draft.destinationUrl(), "destinationUrl"),
				SponsorDestination.optionalHttps(draft.creativeUrl(), "creativeUrl"),
				now);
		return toCampaign(campaignRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public CampaignRecord getCampaign(UUID operatorId, UUID campaignId) {
		operatorGuard.requireOperator(operatorId);
		return toCampaign(campaign(campaignId));
	}

	@Transactional
	public CampaignRecord updateCampaign(UUID operatorId, UUID campaignId, CampaignDraft draft) {
		operatorGuard.requireOperator(operatorId);
		SponsorshipCampaignEntity entity = campaign(campaignId);
		if (CampaignStatus.valueOf(entity.getStatus()) == CampaignStatus.ARCHIVED) {
			throw new SponsorInputInvalidException("Archived campaigns cannot be edited");
		}
		entity.update(
				copy(draft),
				starts(draft.startsAt()),
				ends(draft.startsAt(), draft.endsAt()),
				SponsorCopy.requirePlain(draft.headline(), "headline", 160),
				SponsorCopy.requirePlain(draft.body(), "body", 500),
				SponsorCopy.requirePlain(draft.ctaLabel(), "ctaLabel", 40),
				SponsorDestination.requireHttps(draft.destinationUrl(), "destinationUrl"),
				SponsorDestination.optionalHttps(draft.creativeUrl(), "creativeUrl"),
				clock.instant());
		return toCampaign(campaignRepository.save(entity));
	}

	@Transactional
	public CampaignRecord transitionCampaign(UUID operatorId, UUID campaignId, CampaignStatus requested) {
		operatorGuard.requireOperator(operatorId);
		SponsorshipCampaignEntity entity = campaign(campaignId);
		CampaignStatus current = CampaignStatus.valueOf(entity.getStatus());
		if (!CAMPAIGN_TRANSITIONS.get(current).contains(requested)) {
			throw new SponsorInputInvalidException("Campaign cannot move from " + current + " to " + requested);
		}
		Instant now = clock.instant();
		CampaignStatus stored = CampaignWindow.storedStatus(requested, entity.getStartsAt(), entity.getEndsAt(), now);
		entity.setStatus(stored.name(), now);
		return toCampaign(campaignRepository.save(entity));
	}

	@Transactional
	public PlacementRecord attachPlacement(UUID operatorId, UUID campaignId, PlacementDraft draft) {
		operatorGuard.requireOperator(operatorId);
		SponsorshipCampaignEntity campaign = campaign(campaignId);
		PlacementAudience audience = draft.audience();
		if (audience == PlacementAudience.ORGANIZATION && draft.organizationId() == null) {
			throw new SponsorInputInvalidException("organizationId is required for ORGANIZATION audience");
		}
		if (audience == PlacementAudience.TEAM && draft.teamId() == null) {
			throw new SponsorInputInvalidException("teamId is required for TEAM audience");
		}
		SponsorshipPlacementEntity entity = SponsorshipPlacementEntity.create(
				UUID.randomUUID(),
				campaign.getId(),
				draft.surface().name(),
				audience.name(),
				audience == PlacementAudience.ORGANIZATION ? draft.organizationId() : null,
				audience == PlacementAudience.TEAM ? draft.teamId() : null,
				clock.instant());
		return toPlacement(placementRepository.save(entity));
	}

	private String copy(CampaignDraft draft) {
		return SponsorCopy.requirePlain(draft.name(), "name", 160);
	}

	private Instant starts(Instant startsAt) {
		if (startsAt == null) {
			throw new SponsorInputInvalidException("startsAt is required");
		}
		return startsAt;
	}

	private Instant ends(Instant startsAt, Instant endsAt) {
		if (endsAt != null && !endsAt.isAfter(startsAt)) {
			throw new SponsorInputInvalidException("endsAt must be after startsAt");
		}
		return endsAt;
	}

	private SponsorEntity sponsor(UUID sponsorId) {
		return sponsorRepository.findById(sponsorId).orElseThrow(SponsorCatalogNotFoundException::new);
	}

	private SponsorshipCampaignEntity campaign(UUID campaignId) {
		return campaignRepository.findById(campaignId).orElseThrow(SponsorCatalogNotFoundException::new);
	}

	private SponsorRecord toSponsor(SponsorEntity entity) {
		return new SponsorRecord(
				entity.getId(),
				entity.getName(),
				entity.getDisplayName(),
				SponsorStatus.valueOf(entity.getStatus()),
				entity.getLogoUrl(),
				entity.getWebsiteUrl(),
				entity.getDescription(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	private CampaignRecord toCampaign(SponsorshipCampaignEntity entity) {
		List<PlacementRecord> placements = placementRepository.findAllByCampaignId(entity.getId()).stream()
				.map(this::toPlacement)
				.toList();
		return new CampaignRecord(
				entity.getId(),
				entity.getSponsorId(),
				entity.getName(),
				CampaignStatus.valueOf(entity.getStatus()),
				entity.getStartsAt(),
				entity.getEndsAt(),
				entity.getHeadline(),
				entity.getBody(),
				entity.getCtaLabel(),
				entity.getDestinationUrl(),
				entity.getCreativeUrl(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				placements);
	}

	private PlacementRecord toPlacement(SponsorshipPlacementEntity entity) {
		return new PlacementRecord(
				entity.getId(),
				entity.getCampaignId(),
				PlacementSurface.valueOf(entity.getSurface()),
				PlacementAudience.valueOf(entity.getAudience()),
				entity.getOrganizationId(),
				entity.getTeamId());
	}

	public record SponsorDraft(
			String name,
			String displayName,
			String logoUrl,
			String websiteUrl,
			String description) {
	}

	public record CampaignDraft(
			UUID sponsorId,
			String name,
			CampaignStatus status,
			Instant startsAt,
			Instant endsAt,
			String headline,
			String body,
			String ctaLabel,
			String destinationUrl,
			String creativeUrl) {
	}

	public record PlacementDraft(
			PlacementSurface surface,
			PlacementAudience audience,
			UUID organizationId,
			UUID teamId) {
	}
}
