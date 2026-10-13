package com.devinolabs.uap.marketplace.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListingEntity, UUID> {
}
