package com.devinolabs.uap.marketplace.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceCategoryEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceCategoryRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingRepository;
import com.devinolabs.uap.sponsorship.api.PartnerPresentationRules;
import com.devinolabs.uap.sponsorship.api.SponsorOperatorPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort.PartnerRef;

@Service
public class MarketplaceCatalogService {

	private final SponsorOperatorPort operatorPort;
	private final SponsorPartnerPort partnerPort;
	private final MarketplaceCategoryRepository categoryRepository;
	private final MarketplaceListingRepository listingRepository;
	private final Clock clock;

	public MarketplaceCatalogService(
			SponsorOperatorPort operatorPort,
			SponsorPartnerPort partnerPort,
			MarketplaceCategoryRepository categoryRepository,
			MarketplaceListingRepository listingRepository,
			Clock clock) {
		this.operatorPort = operatorPort;
		this.partnerPort = partnerPort;
		this.categoryRepository = categoryRepository;
		this.listingRepository = listingRepository;
		this.clock = clock;
	}

	public record ListingDraft(
			UUID partnerId,
			String title,
			String shortDescription,
			String fullDescription,
			ListingType listingType,
			String categoryCode,
			ListingStatus status,
			boolean featured,
			ListingAudience audience,
			UUID organizationId,
			UUID teamId,
			String imageUrl,
			String destinationUrl,
			String priceDisplay,
			String discountText,
			String promoCode,
			Instant startsAt,
			Instant endsAt,
			Instant offerStartsAt,
			Instant offerEndsAt) {
	}

	@Transactional(readOnly = true)
	public List<CategoryRecord> categories() {
		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.map(category -> new CategoryRecord(category.getCode(), category.getName(), category.getSortOrder()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OperatorListingRecord> listForOperator(UUID operatorId) {
		operatorPort.requireOperator(operatorId);
		return listingRepository.findAll().stream().map(this::toOperator).toList();
	}

	@Transactional(readOnly = true)
	public OperatorListingRecord getForOperator(UUID operatorId, UUID listingId) {
		operatorPort.requireOperator(operatorId);
		return toOperator(listing(listingId));
	}

	@Transactional
	public OperatorListingRecord create(UUID operatorId, ListingDraft draft) {
		operatorPort.requireOperator(operatorId);
		Instant now = clock.instant();
		ValidatedListing validated = validate(draft, now);
		MarketplaceListingEntity entity = MarketplaceListingEntity.create(
				UUID.randomUUID(),
				validated.partnerId(),
				validated.title(),
				validated.shortDescription(),
				validated.fullDescription(),
				validated.listingType().name(),
				validated.categoryCode(),
				validated.status().name(),
				validated.imageUrl(),
				validated.destinationUrl(),
				validated.priceDisplay(),
				validated.discountText(),
				validated.promoCode(),
				validated.featured(),
				validated.audience().name(),
				validated.organizationId(),
				validated.teamId(),
				validated.startsAt(),
				validated.endsAt(),
				validated.offerStartsAt(),
				validated.offerEndsAt(),
				now);
		return toOperator(listingRepository.save(entity));
	}

	@Transactional
	public OperatorListingRecord update(UUID operatorId, UUID listingId, ListingDraft draft) {
		operatorPort.requireOperator(operatorId);
		MarketplaceListingEntity entity = listing(listingId);
		Instant now = clock.instant();
		ValidatedListing validated = validate(draft, now);
		entity.replace(
				validated.partnerId(),
				validated.title(),
				validated.shortDescription(),
				validated.fullDescription(),
				validated.listingType().name(),
				validated.categoryCode(),
				validated.imageUrl(),
				validated.destinationUrl(),
				validated.priceDisplay(),
				validated.discountText(),
				validated.promoCode(),
				validated.featured(),
				validated.audience().name(),
				validated.organizationId(),
				validated.teamId(),
				validated.startsAt(),
				validated.endsAt(),
				validated.offerStartsAt(),
				validated.offerEndsAt(),
				now);
		entity.setStatus(validated.status().name(), now);
		return toOperator(entity);
	}

	@Transactional
	public OperatorListingRecord transition(UUID operatorId, UUID listingId, ListingStatus status) {
		operatorPort.requireOperator(operatorId);
		if (status == null) {
			throw new MarketplaceInputInvalidException("status is required");
		}
		MarketplaceListingEntity entity = listing(listingId);
		Instant now = clock.instant();
		PartnerRef partner = partnerPort.findPartner(entity.getSponsorId())
				.orElseThrow(() -> new MarketplaceInputInvalidException("partner was not found"));
		if ((status == ListingStatus.ACTIVE || status == ListingStatus.SCHEDULED)
				&& !"ACTIVE".equals(partner.status())) {
			throw new MarketplaceInputInvalidException("partner must be ACTIVE before the listing can go live");
		}
		ListingStatus stored = ListingWindow.storedStatus(status, entity.getStartsAt(), entity.getEndsAt(), now);
		entity.setStatus(stored.name(), now);
		return toOperator(entity);
	}

	private record ValidatedListing(
			UUID partnerId,
			String title,
			String shortDescription,
			String fullDescription,
			ListingType listingType,
			String categoryCode,
			ListingStatus status,
			boolean featured,
			ListingAudience audience,
			UUID organizationId,
			UUID teamId,
			String imageUrl,
			String destinationUrl,
			String priceDisplay,
			String discountText,
			String promoCode,
			Instant startsAt,
			Instant endsAt,
			Instant offerStartsAt,
			Instant offerEndsAt) {
	}

	private ValidatedListing validate(ListingDraft draft, Instant now) {
		if (draft.partnerId() == null) {
			throw new MarketplaceInputInvalidException("partnerId is required");
		}
		PartnerRef partner = partnerPort.findPartner(draft.partnerId())
				.orElseThrow(() -> new MarketplaceInputInvalidException("partner was not found"));
		if (draft.listingType() == null) {
			throw new MarketplaceInputInvalidException("listingType is required");
		}
		if (draft.audience() == null) {
			throw new MarketplaceInputInvalidException("audience is required");
		}
		if (draft.startsAt() == null) {
			throw new MarketplaceInputInvalidException("startsAt is required");
		}
		if (draft.endsAt() != null && !draft.startsAt().isBefore(draft.endsAt())) {
			throw new MarketplaceInputInvalidException("endsAt must be after startsAt");
		}
		if (draft.offerStartsAt() != null && draft.offerEndsAt() != null
				&& !draft.offerStartsAt().isBefore(draft.offerEndsAt())) {
			throw new MarketplaceInputInvalidException("offerEndsAt must be after offerStartsAt");
		}
		category(draft.categoryCode());
		String title = plain(draft.title(), "title", 160);
		String shortDescription = plain(draft.shortDescription(), "shortDescription", 280);
		String fullDescription = plain(draft.fullDescription(), "fullDescription", 2000);
		String destinationUrl = https(draft.destinationUrl(), "destinationUrl");
		String imageUrl = optionalHttps(draft.imageUrl(), "imageUrl");
		String priceDisplay = optionalPlain(draft.priceDisplay(), "priceDisplay", 40);
		String discountText = optionalPlain(draft.discountText(), "discountText", 160);
		String promoCode = optionalPlain(draft.promoCode(), "promoCode", 40);
		if (draft.audience() == ListingAudience.ORGANIZATION && draft.organizationId() == null) {
			throw new MarketplaceInputInvalidException("organizationId is required for ORGANIZATION audience");
		}
		if (draft.audience() == ListingAudience.TEAM && draft.teamId() == null) {
			throw new MarketplaceInputInvalidException("teamId is required for TEAM audience");
		}
		ListingStatus requested = draft.status() == null ? ListingStatus.DRAFT : draft.status();
		if ((requested == ListingStatus.ACTIVE || requested == ListingStatus.SCHEDULED)
				&& !"ACTIVE".equals(partner.status())) {
			throw new MarketplaceInputInvalidException("partner must be ACTIVE before the listing can go live");
		}
		ListingStatus stored = ListingWindow.storedStatus(requested, draft.startsAt(), draft.endsAt(), now);
		return new ValidatedListing(
				draft.partnerId(),
				title,
				shortDescription,
				fullDescription,
				draft.listingType(),
				draft.categoryCode().trim(),
				stored,
				draft.featured(),
				draft.audience(),
				draft.organizationId(),
				draft.teamId(),
				imageUrl,
				destinationUrl,
				priceDisplay,
				discountText,
				promoCode,
				draft.startsAt(),
				draft.endsAt(),
				draft.offerStartsAt(),
				draft.offerEndsAt());
	}

	private static String plain(String value, String field, int max) {
		try {
			return PartnerPresentationRules.requirePlain(value, field, max);
		}
		catch (IllegalArgumentException ex) {
			throw new MarketplaceInputInvalidException(ex.getMessage());
		}
	}

	private static String optionalPlain(String value, String field, int max) {
		try {
			return PartnerPresentationRules.optionalPlain(value, field, max);
		}
		catch (IllegalArgumentException ex) {
			throw new MarketplaceInputInvalidException(ex.getMessage());
		}
	}

	private static String https(String value, String field) {
		try {
			return PartnerPresentationRules.requireHttps(value, field);
		}
		catch (IllegalArgumentException ex) {
			throw new MarketplaceInputInvalidException(ex.getMessage());
		}
	}

	private static String optionalHttps(String value, String field) {
		try {
			return PartnerPresentationRules.optionalHttps(value, field);
		}
		catch (IllegalArgumentException ex) {
			throw new MarketplaceInputInvalidException(ex.getMessage());
		}
	}

	private MarketplaceCategoryEntity category(String code) {
		if (code == null || code.isBlank()) {
			throw new MarketplaceInputInvalidException("categoryCode is required");
		}
		return categoryRepository.findById(code)
				.orElseThrow(() -> new MarketplaceInputInvalidException("unknown category"));
	}

	private MarketplaceListingEntity listing(UUID listingId) {
		return listingRepository.findById(listingId).orElseThrow(MarketplaceCatalogNotFoundException::new);
	}

	private OperatorListingRecord toOperator(MarketplaceListingEntity entity) {
		return new OperatorListingRecord(
				entity.getId(),
				entity.getSponsorId(),
				entity.getTitle(),
				entity.getShortDescription(),
				entity.getFullDescription(),
				ListingType.valueOf(entity.getListingType()),
				entity.getCategoryCode(),
				ListingStatus.valueOf(entity.getStatus()),
				entity.isFeatured(),
				ListingAudience.valueOf(entity.getAudience()),
				entity.getOrganizationId(),
				entity.getTeamId(),
				entity.getImageUrl(),
				entity.getDestinationUrl(),
				entity.getPriceDisplay(),
				entity.getDiscountText(),
				entity.getPromoCode(),
				entity.getStartsAt(),
				entity.getEndsAt(),
				entity.getOfferStartsAt(),
				entity.getOfferEndsAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
