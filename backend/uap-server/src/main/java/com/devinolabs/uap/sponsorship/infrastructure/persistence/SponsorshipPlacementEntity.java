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
@Table(name = "sponsorship_placements")
public class SponsorshipPlacementEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "campaign_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID campaignId;

	@Column(nullable = false, length = 16)
	private String surface;

	@Column(nullable = false, length = 16)
	private String audience;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", columnDefinition = "BINARY(16)")
	private UUID teamId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected SponsorshipPlacementEntity() {
	}

	public static SponsorshipPlacementEntity create(
			UUID id,
			UUID campaignId,
			String surface,
			String audience,
			UUID organizationId,
			UUID teamId,
			Instant now) {
		SponsorshipPlacementEntity entity = new SponsorshipPlacementEntity();
		entity.id = id;
		entity.campaignId = campaignId;
		entity.surface = surface;
		entity.audience = audience;
		entity.organizationId = organizationId;
		entity.teamId = teamId;
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

	public String getAudience() {
		return audience;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public UUID getTeamId() {
		return teamId;
	}
}
