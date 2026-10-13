package com.devinolabs.uap.marketplace.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategoryEntity, String> {

	List<MarketplaceCategoryEntity> findAllByOrderBySortOrderAsc();
}
