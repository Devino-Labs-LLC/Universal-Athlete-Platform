package com.devinolabs.uap.sponsorship.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SponsorshipEventRepository extends JpaRepository<SponsorshipEventEntity, UUID> {

	Optional<SponsorshipEventEntity> findByAccountIdAndCampaignIdAndSurfaceAndKindAndBucket(
			UUID accountId,
			UUID campaignId,
			String surface,
			String kind,
			Instant bucket);

	@Query("""
			select e.campaignId, e.surface, e.kind, count(e)
			from SponsorshipEventEntity e
			group by e.campaignId, e.surface, e.kind
			""")
	List<Object[]> aggregateCounts();
}
