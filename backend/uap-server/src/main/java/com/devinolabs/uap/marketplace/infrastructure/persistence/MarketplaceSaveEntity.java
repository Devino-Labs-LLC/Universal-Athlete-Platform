package com.devinolabs.uap.marketplace.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "marketplace_saves")
public class MarketplaceSaveEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "listing_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID listingId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected MarketplaceSaveEntity() {
	}

	public static MarketplaceSaveEntity create(UUID id, UUID accountId, UUID listingId, Instant now) {
		MarketplaceSaveEntity entity = new MarketplaceSaveEntity();
		entity.id = id;
		entity.accountId = accountId;
		entity.listingId = listingId;
		entity.createdAt = now;
		entity.isNew = true;
		return entity;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public UUID getListingId() {
		return listingId;
	}
}
