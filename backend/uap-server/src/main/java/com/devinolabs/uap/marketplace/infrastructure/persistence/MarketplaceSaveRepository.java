package com.devinolabs.uap.marketplace.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceSaveRepository extends JpaRepository<MarketplaceSaveEntity, UUID> {

	Optional<MarketplaceSaveEntity> findByAccountIdAndListingId(UUID accountId, UUID listingId);

	List<MarketplaceSaveEntity> findAllByAccountIdOrderByCreatedAtDesc(UUID accountId);

	void deleteByAccountIdAndListingId(UUID accountId, UUID listingId);
}
