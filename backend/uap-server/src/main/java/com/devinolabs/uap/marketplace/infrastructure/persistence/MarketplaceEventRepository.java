package com.devinolabs.uap.marketplace.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketplaceEventRepository extends JpaRepository<MarketplaceEventEntity, UUID> {

	Optional<MarketplaceEventEntity> findByAccountIdAndListingIdAndKindAndBucket(
			UUID accountId,
			UUID listingId,
			String kind,
			Instant bucket);

	@Query("""
			select e.listingId, e.kind, count(e)
			from MarketplaceEventEntity e
			group by e.listingId, e.kind
			""")
	List<Object[]> aggregateCounts();
}
