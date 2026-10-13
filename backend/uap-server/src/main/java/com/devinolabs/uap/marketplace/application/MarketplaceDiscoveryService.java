package com.devinolabs.uap.marketplace.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceCategoryEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceCategoryRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceEventEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceEventRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceSaveEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceSaveRepository;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort.PartnerRef;

@Service
public class MarketplaceDiscoveryService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	private final OrganizationMembershipPort membershipPort;
	private final SponsorPartnerPort partnerPort;
	private final MarketplaceListingRepository listingRepository;
	private final MarketplaceCategoryRepository categoryRepository;
	private final MarketplaceSaveRepository saveRepository;
	private final MarketplaceEventRepository eventRepository;
	private final Clock clock;

	public MarketplaceDiscoveryService(
			OrganizationMembershipPort membershipPort,
			SponsorPartnerPort partnerPort,
			MarketplaceListingRepository listingRepository,
			MarketplaceCategoryRepository categoryRepository,
			MarketplaceSaveRepository saveRepository,
			MarketplaceEventRepository eventRepository,
			Clock clock) {
		this.membershipPort = membershipPort;
		this.partnerPort = partnerPort;
		this.listingRepository = listingRepository;
		this.categoryRepository = categoryRepository;
		this.saveRepository = saveRepository;
		this.eventRepository = eventRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ListingPage browse(
			UUID accountId,
			String categoryCode,
			ListingType listingType,
			String query,
			Boolean featured,
			int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		Instant now = clock.instant();
		Set<UUID> saved = savedIds(accountId);
		List<ListingRecord> matches = listingRepository.findAll().stream()
				.filter(listing -> visible(accountId, listing, now))
				.filter(listing -> categoryCode == null || categoryCode.isBlank()
						|| categoryCode.equals(listing.getCategoryCode()))
				.filter(listing -> listingType == null || listingType.name().equals(listing.getListingType()))
				.filter(listing -> featured == null || listing.isFeatured() == featured)
				.filter(listing -> matchesQuery(listing, query))
				.sorted(Comparator
						.comparing(MarketplaceListingEntity::isFeatured).reversed()
						.thenComparing(MarketplaceListingEntity::getUpdatedAt, Comparator.reverseOrder()))
				.map(listing -> toAthlete(listing, now, saved.contains(listing.getId()), true))
				.flatMap(Optional::stream)
				.toList();
		int from = Math.min(safePage * safeSize, matches.size());
		int to = Math.min(from + safeSize, matches.size());
		return new ListingPage(matches.subList(from, to), safePage, safeSize, matches.size());
	}

	@Transactional(readOnly = true)
	public ListingRecord detail(UUID accountId, UUID listingId) {
		MarketplaceListingEntity listing = listingRepository.findById(listingId)
				.orElseThrow(MarketplaceCatalogNotFoundException::new);
		Instant now = clock.instant();
		if (!visible(accountId, listing, now)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		return toAthlete(listing, now, saved(accountId, listingId), true)
				.orElseThrow(MarketplaceCatalogNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public List<ListingRecord> saves(UUID accountId) {
		Instant now = clock.instant();
		Set<UUID> saved = savedIds(accountId);
		return saveRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId).stream()
				.map(save -> listingRepository.findById(save.getListingId()))
				.flatMap(Optional::stream)
				.map(listing -> toAthlete(listing, now, saved.contains(listing.getId()), visible(accountId, listing, now)))
				.flatMap(Optional::stream)
				.toList();
	}

	@Transactional
	public void save(UUID accountId, UUID listingId) {
		MarketplaceListingEntity listing = listingRepository.findById(listingId)
				.orElseThrow(MarketplaceCatalogNotFoundException::new);
		Instant now = clock.instant();
		if (!visible(accountId, listing, now)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		if (saveRepository.findByAccountIdAndListingId(accountId, listingId).isPresent()) {
			return;
		}
		try {
			saveRepository.save(MarketplaceSaveEntity.create(UUID.randomUUID(), accountId, listingId, now));
		}
		catch (DataIntegrityViolationException ignored) {
			return;
		}
		record(accountId, listingId, MarketplaceEventKind.SAVE);
	}

	@Transactional
	public void unsave(UUID accountId, UUID listingId) {
		saveRepository.deleteByAccountIdAndListingId(accountId, listingId);
	}

	@Transactional
	public void record(UUID accountId, UUID listingId, MarketplaceEventKind kind) {
		MarketplaceListingEntity listing = listingRepository.findById(listingId)
				.orElseThrow(MarketplaceCatalogNotFoundException::new);
		Instant now = clock.instant();
		if (!visible(accountId, listing, now)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		if (kind == MarketplaceEventKind.REDEEM && !ListingWindow.offerLive(listing, now)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		Instant bucket = now.truncatedTo(ChronoUnit.HOURS);
		if (eventRepository.findByAccountIdAndListingIdAndKindAndBucket(accountId, listingId, kind.name(), bucket)
				.isPresent()) {
			return;
		}
		try {
			eventRepository.save(MarketplaceEventEntity.create(
					UUID.randomUUID(), listingId, kind.name(), accountId, bucket, now));
		}
		catch (DataIntegrityViolationException ignored) {
			// Concurrent duplicate in the same hour bucket.
		}
	}

	private boolean visible(UUID accountId, MarketplaceListingEntity listing, Instant now) {
		if (!ListingWindow.presentable(listing, now)) {
			return false;
		}
		Optional<PartnerRef> partner = partnerPort.findPartner(listing.getSponsorId());
		if (partner.isEmpty() || !"ACTIVE".equals(partner.get().status())) {
			return false;
		}
		return switch (ListingAudience.valueOf(listing.getAudience())) {
			case ALL -> true;
			case ORGANIZATION -> listing.getOrganizationId() != null
					&& membershipPort.hasActiveOrganizationMembership(accountId, listing.getOrganizationId());
			case TEAM -> listing.getTeamId() != null
					&& membershipPort.findActiveTeamMembership(accountId, listing.getTeamId()).isPresent();
		};
	}

	private static boolean matchesQuery(MarketplaceListingEntity listing, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String needle = query.trim().toLowerCase(Locale.ROOT);
		return listing.getTitle().toLowerCase(Locale.ROOT).contains(needle)
				|| listing.getShortDescription().toLowerCase(Locale.ROOT).contains(needle)
				|| listing.getFullDescription().toLowerCase(Locale.ROOT).contains(needle);
	}

	private Optional<ListingRecord> toAthlete(
			MarketplaceListingEntity listing,
			Instant now,
			boolean saved,
			boolean currentlyVisible) {
		Optional<PartnerRef> partner = partnerPort.findPartner(listing.getSponsorId());
		if (partner.isEmpty()) {
			return Optional.empty();
		}
		boolean offerActive = currentlyVisible && ListingWindow.offerLive(listing, now);
		String categoryName = categoryRepository.findById(listing.getCategoryCode())
				.map(MarketplaceCategoryEntity::getName)
				.orElse(listing.getCategoryCode());
		return Optional.of(new ListingRecord(
				listing.getId(),
				partner.get().id(),
				partner.get().displayName(),
				listing.getTitle(),
				listing.getShortDescription(),
				currentlyVisible ? listing.getFullDescription() : listing.getShortDescription(),
				ListingType.valueOf(listing.getListingType()),
				listing.getCategoryCode(),
				categoryName,
				listing.isFeatured(),
				currentlyVisible,
				offerActive,
				currentlyVisible ? listing.getImageUrl() : null,
				currentlyVisible ? listing.getDestinationUrl() : null,
				currentlyVisible ? listing.getPriceDisplay() : null,
				currentlyVisible && offerActive ? listing.getDiscountText() : null,
				currentlyVisible && offerActive ? listing.getPromoCode() : null,
				saved));
	}

	private Set<UUID> savedIds(UUID accountId) {
		return saveRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId).stream()
				.map(MarketplaceSaveEntity::getListingId)
				.collect(Collectors.toSet());
	}

	private boolean saved(UUID accountId, UUID listingId) {
		return saveRepository.findByAccountIdAndListingId(accountId, listingId).isPresent();
	}
}
