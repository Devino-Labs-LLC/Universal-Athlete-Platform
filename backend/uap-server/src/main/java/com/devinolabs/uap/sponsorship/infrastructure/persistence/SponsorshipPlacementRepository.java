package com.devinolabs.uap.sponsorship.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorshipPlacementRepository extends JpaRepository<SponsorshipPlacementEntity, UUID> {

	List<SponsorshipPlacementEntity> findAllBySurface(String surface);

	List<SponsorshipPlacementEntity> findAllByCampaignId(UUID campaignId);
}
