package com.devinolabs.uap.marketplace.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketplace_categories")
public class MarketplaceCategoryEntity {

	@Id
	@Column(nullable = false, length = 32)
	private String code;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected MarketplaceCategoryEntity() {
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
