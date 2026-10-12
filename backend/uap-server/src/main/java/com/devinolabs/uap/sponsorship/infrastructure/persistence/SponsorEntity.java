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
@Table(name = "sponsors")
public class SponsorEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(nullable = false, length = 16)
	private String status;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	@Column(name = "website_url", length = 500)
	private String websiteUrl;

	@Column(length = 500)
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private boolean isNew = true;

	protected SponsorEntity() {
	}

	public static SponsorEntity create(
			UUID id,
			String name,
			String displayName,
			String status,
			String logoUrl,
			String websiteUrl,
			String description,
			Instant now) {
		SponsorEntity entity = new SponsorEntity();
		entity.id = id;
		entity.name = name;
		entity.displayName = displayName;
		entity.status = status;
		entity.logoUrl = logoUrl;
		entity.websiteUrl = websiteUrl;
		entity.description = description;
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

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getStatus() {
		return status;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public String getDescription() {
		return description;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(
			String name,
			String displayName,
			String logoUrl,
			String websiteUrl,
			String description,
			Instant now) {
		this.name = name;
		this.displayName = displayName;
		this.logoUrl = logoUrl;
		this.websiteUrl = websiteUrl;
		this.description = description;
		this.updatedAt = now;
	}

	public void setStatus(String status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}
}
