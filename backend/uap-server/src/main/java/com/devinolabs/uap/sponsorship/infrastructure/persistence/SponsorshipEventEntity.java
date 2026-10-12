package com.devinolabs.uap.sponsorship.infrastructure.persistence;

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
@Table(name = "sponsorship_events")
public class SponsorshipEventEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "campaign_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID campaignId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "placement_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID placementId;

	@Column(nullable = false, length = 16)
	private String surface;

	@Column(nullable = false, length = 16)
	private String kind;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@Column(nullable = false)
	private Instant bucket;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected SponsorshipEventEntity() {
	}

	public static SponsorshipEventEntity create(
			UUID id,
			UUID campaignId,
			UUID placementId,
			String surface,
			String kind,
			UUID accountId,
			Instant bucket,
			Instant now) {
		SponsorshipEventEntity entity = new SponsorshipEventEntity();
		entity.id = id;
		entity.campaignId = campaignId;
		entity.placementId = placementId;
		entity.surface = surface;
		entity.kind = kind;
		entity.accountId = accountId;
		entity.bucket = bucket;
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

	public UUID getCampaignId() {
		return campaignId;
	}

	public String getSurface() {
		return surface;
	}

	public String getKind() {
		return kind;
	}

	public Instant getBucket() {
		return bucket;
	}
}
