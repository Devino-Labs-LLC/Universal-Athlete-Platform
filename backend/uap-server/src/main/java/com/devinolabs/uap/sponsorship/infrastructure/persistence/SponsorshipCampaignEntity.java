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
@Table(name = "sponsorship_campaigns")
public class SponsorshipCampaignEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "sponsor_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID sponsorId;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, length = 16)
	private String status;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(nullable = false, length = 160)
	private String headline;

	@Column(nullable = false, length = 500)
	private String body;

	@Column(name = "cta_label", nullable = false, length = 40)
	private String ctaLabel;

	@Column(name = "destination_url", nullable = false, length = 500)
	private String destinationUrl;

	@Column(name = "creative_url", length = 500)
	private String creativeUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private boolean isNew = true;

	protected SponsorshipCampaignEntity() {
	}

	public static SponsorshipCampaignEntity create(
			UUID id,
			UUID sponsorId,
			String name,
			String status,
			Instant startsAt,
			Instant endsAt,
			String headline,
			String body,
			String ctaLabel,
			String destinationUrl,
			String creativeUrl,
			Instant now) {
		SponsorshipCampaignEntity entity = new SponsorshipCampaignEntity();
		entity.id = id;
		entity.sponsorId = sponsorId;
		entity.name = name;
		entity.status = status;
		entity.startsAt = startsAt;
		entity.endsAt = endsAt;
		entity.headline = headline;
		entity.body = body;
		entity.ctaLabel = ctaLabel;
		entity.destinationUrl = destinationUrl;
		entity.creativeUrl = creativeUrl;
		entity.createdAt = now;
		entity.updatedAt = now;
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

	public UUID getSponsorId() {
		return sponsorId;
	}

	public String getName() {
		return name;
	}

	public String getStatus() {
		return status;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public String getHeadline() {
		return headline;
	}

	public String getBody() {
		return body;
	}

	public String getCtaLabel() {
		return ctaLabel;
	}

	public String getDestinationUrl() {
		return destinationUrl;
	}

	public String getCreativeUrl() {
		return creativeUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(
			String name,
			Instant startsAt,
			Instant endsAt,
			String headline,
			String body,
			String ctaLabel,
			String destinationUrl,
			String creativeUrl,
			Instant now) {
		this.name = name;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.headline = headline;
		this.body = body;
		this.ctaLabel = ctaLabel;
		this.destinationUrl = destinationUrl;
		this.creativeUrl = creativeUrl;
		this.updatedAt = now;
	}

	public void setStatus(String status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}
}
