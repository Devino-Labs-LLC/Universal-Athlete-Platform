package com.devinolabs.uap.sponsorship.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorshipCampaignRepository extends JpaRepository<SponsorshipCampaignEntity, UUID> {

	List<SponsorshipCampaignEntity> findAllByOrderByUpdatedAtDesc();

	List<SponsorshipCampaignEntity> findAllBySponsorIdOrderByUpdatedAtDesc(UUID sponsorId);
}
