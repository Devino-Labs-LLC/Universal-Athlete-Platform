package com.devinolabs.uap.marketplace.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceEventRepository;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingEntity;
import com.devinolabs.uap.marketplace.infrastructure.persistence.MarketplaceListingRepository;
import com.devinolabs.uap.sponsorship.api.SponsorOperatorPort;
import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort;

@Service
public class MarketplaceReportingService {

	private final SponsorOperatorPort operatorPort;
	private final SponsorPartnerPort partnerPort;
	private final MarketplaceEventRepository eventRepository;
	private final MarketplaceListingRepository listingRepository;

	public MarketplaceReportingService(
			SponsorOperatorPort operatorPort,
			SponsorPartnerPort partnerPort,
			MarketplaceEventRepository eventRepository,
			MarketplaceListingRepository listingRepository) {
		this.operatorPort = operatorPort;
		this.partnerPort = partnerPort;
		this.eventRepository = eventRepository;
		this.listingRepository = listingRepository;
	}

	@Transactional(readOnly = true)
	public List<MarketplaceReportRow> report(UUID operatorId) {
		operatorPort.requireOperator(operatorId);
		Map<UUID, MarketplaceListingEntity> listings = new HashMap<>();
		listingRepository.findAll().forEach(listing -> listings.put(listing.getId(), listing));
		record Counts(long impressions, long details, long clicks, long redemptions, long saves) {
			Counts add(String kind, long count) {
				return switch (kind) {
					case "DETAIL" -> new Counts(impressions, details + count, clicks, redemptions, saves);
					case "CLICK" -> new Counts(impressions, details, clicks + count, redemptions, saves);
					case "REDEEM" -> new Counts(impressions, details, clicks, redemptions + count, saves);
					case "SAVE" -> new Counts(impressions, details, clicks, redemptions, saves + count);
					default -> new Counts(impressions + count, details, clicks, redemptions, saves);
				};
			}
		}
		Map<UUID, Counts> grouped = new HashMap<>();
		for (Object[] row : eventRepository.aggregateCounts()) {
			UUID listingId = (UUID) row[0];
			grouped.merge(listingId, new Counts(0, 0, 0, 0, 0).add((String) row[1], (Long) row[2]),
					(left, right) -> new Counts(
							left.impressions() + right.impressions(),
							left.details() + right.details(),
							left.clicks() + right.clicks(),
							left.redemptions() + right.redemptions(),
							left.saves() + right.saves()));
		}
		List<MarketplaceReportRow> rows = new ArrayList<>();
		grouped.forEach((listingId, counts) -> {
			MarketplaceListingEntity listing = listings.get(listingId);
			if (listing == null) {
				return;
			}
			var partner = partnerPort.findPartner(listing.getSponsorId());
			if (partner.isEmpty()) {
				return;
			}
			double ctr = counts.impressions() == 0 ? 0d : (double) counts.clicks() / counts.impressions();
			rows.add(new MarketplaceReportRow(
					listing.getId(),
					listing.getTitle(),
					partner.get().id(),
					partner.get().displayName(),
					listing.getCategoryCode(),
					ListingType.valueOf(listing.getListingType()),
					counts.impressions(),
					counts.details(),
					counts.clicks(),
					counts.redemptions(),
					counts.saves(),
					ctr));
		});
		return rows;
	}
}
