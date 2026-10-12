package com.devinolabs.uap.sponsorship.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignEntity;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipCampaignRepository;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorshipEventRepository;

@Service
public class SponsorshipReportingService {

	private final SponsorOperatorGuard operatorGuard;
	private final SponsorshipEventRepository eventRepository;
	private final SponsorshipCampaignRepository campaignRepository;
	private final SponsorRepository sponsorRepository;

	public SponsorshipReportingService(
			SponsorOperatorGuard operatorGuard,
			SponsorshipEventRepository eventRepository,
			SponsorshipCampaignRepository campaignRepository,
			SponsorRepository sponsorRepository) {
		this.operatorGuard = operatorGuard;
		this.eventRepository = eventRepository;
		this.campaignRepository = campaignRepository;
		this.sponsorRepository = sponsorRepository;
	}

	@Transactional(readOnly = true)
	public List<SponsorshipReportRow> report(UUID operatorId) {
		operatorGuard.requireOperator(operatorId);
		Map<UUID, SponsorshipCampaignEntity> campaigns = campaignRepository.findAll().stream()
				.collect(Collectors.toMap(SponsorshipCampaignEntity::getId, campaign -> campaign));
		Map<UUID, SponsorEntity> sponsors = sponsorRepository.findAll().stream()
				.collect(Collectors.toMap(SponsorEntity::getId, sponsor -> sponsor));
		record Key(UUID campaignId, String surface) {
		}
		record Counts(long impressions, long clicks) {
			Counts add(String kind, long count) {
				if (SponsorshipEventKind.CLICK.name().equals(kind)) {
					return new Counts(impressions, clicks + count);
				}
				return new Counts(impressions + count, clicks);
			}
		}
		Map<Key, Counts> grouped = new java.util.HashMap<>();
		for (Object[] row : eventRepository.aggregateCounts()) {
			Key key = new Key((UUID) row[0], (String) row[1]);
			grouped.merge(key, new Counts(0, 0).add((String) row[2], (Long) row[3]), (left, right) ->
					new Counts(left.impressions() + right.impressions(), left.clicks() + right.clicks()));
		}
		List<SponsorshipReportRow> rows = new ArrayList<>();
		grouped.forEach((key, counts) -> {
			SponsorshipCampaignEntity campaign = campaigns.get(key.campaignId());
			if (campaign == null) {
				return;
			}
			SponsorEntity sponsor = sponsors.get(campaign.getSponsorId());
			if (sponsor == null) {
				return;
			}
			double ctr = counts.impressions() == 0 ? 0d : (double) counts.clicks() / counts.impressions();
			rows.add(new SponsorshipReportRow(
					sponsor.getId(),
					sponsor.getName(),
					campaign.getId(),
					campaign.getName(),
					PlacementSurface.valueOf(key.surface()),
					counts.impressions(),
					counts.clicks(),
					ctr));
		});
		return rows;
	}
}
