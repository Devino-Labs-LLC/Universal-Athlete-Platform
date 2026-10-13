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
@Table(name = "marketplace_listings")
public class MarketplaceListingEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "sponsor_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID sponsorId;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(name = "short_description", nullable = false, length = 280)
	private String shortDescription;

	@Column(name = "full_description", nullable = false, length = 2000)
	private String fullDescription;

	@Column(name = "listing_type", nullable = false, length = 16)
	private String listingType;

	@Column(name = "category_code", nullable = false, length = 32)
	private String categoryCode;

	@Column(nullable = false, length = 16)
	private String status;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "destination_url", nullable = false, length = 500)
	private String destinationUrl;

	@Column(name = "price_display", length = 40)
	private String priceDisplay;

	@Column(name = "discount_text", length = 160)
	private String discountText;

	@Column(name = "promo_code", length = 40)
	private String promoCode;

	@Column(nullable = false)
	private boolean featured;

	@Column(nullable = false, length = 16)
	private String audience;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", columnDefinition = "BINARY(16)")
	private UUID teamId;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "offer_starts_at")
	private Instant offerStartsAt;

	@Column(name = "offer_ends_at")
	private Instant offerEndsAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private boolean isNew = true;

	protected MarketplaceListingEntity() {
	}

	public static MarketplaceListingEntity create(
			UUID id,
			UUID sponsorId,
			String title,
			String shortDescription,
			String fullDescription,
			String listingType,
			String categoryCode,
			String status,
			String imageUrl,
			String destinationUrl,
			String priceDisplay,
			String discountText,
			String promoCode,
			boolean featured,
			String audience,
			UUID organizationId,
			UUID teamId,
			Instant startsAt,
			Instant endsAt,
			Instant offerStartsAt,
			Instant offerEndsAt,
			Instant now) {
		MarketplaceListingEntity entity = new MarketplaceListingEntity();
		entity.id = id;
		entity.sponsorId = sponsorId;
		entity.title = title;
		entity.shortDescription = shortDescription;
		entity.fullDescription = fullDescription;
		entity.listingType = listingType;
		entity.categoryCode = categoryCode;
		entity.status = status;
		entity.imageUrl = imageUrl;
		entity.destinationUrl = destinationUrl;
		entity.priceDisplay = priceDisplay;
		entity.discountText = discountText;
		entity.promoCode = promoCode;
		entity.featured = featured;
		entity.audience = audience;
		entity.organizationId = organizationId;
		entity.teamId = teamId;
		entity.startsAt = startsAt;
		entity.endsAt = endsAt;
		entity.offerStartsAt = offerStartsAt;
		entity.offerEndsAt = offerEndsAt;
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

	public String getTitle() {
		return title;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public String getFullDescription() {
		return fullDescription;
	}

	public String getListingType() {
		return listingType;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public String getStatus() {
		return status;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getDestinationUrl() {
		return destinationUrl;
	}

	public String getPriceDisplay() {
		return priceDisplay;
	}

	public String getDiscountText() {
		return discountText;
	}

	public String getPromoCode() {
		return promoCode;
	}

	public boolean isFeatured() {
		return featured;
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

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public Instant getOfferStartsAt() {
		return offerStartsAt;
	}

	public Instant getOfferEndsAt() {
		return offerEndsAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void replace(
			UUID sponsorId,
			String title,
			String shortDescription,
			String fullDescription,
			String listingType,
			String categoryCode,
			String imageUrl,
			String destinationUrl,
			String priceDisplay,
			String discountText,
			String promoCode,
			boolean featured,
			String audience,
			UUID organizationId,
			UUID teamId,
			Instant startsAt,
			Instant endsAt,
			Instant offerStartsAt,
			Instant offerEndsAt,
			Instant now) {
		this.sponsorId = sponsorId;
		this.title = title;
		this.shortDescription = shortDescription;
		this.fullDescription = fullDescription;
		this.listingType = listingType;
		this.categoryCode = categoryCode;
		this.imageUrl = imageUrl;
		this.destinationUrl = destinationUrl;
		this.priceDisplay = priceDisplay;
		this.discountText = discountText;
		this.promoCode = promoCode;
		this.featured = featured;
		this.audience = audience;
		this.organizationId = organizationId;
		this.teamId = teamId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.offerStartsAt = offerStartsAt;
		this.offerEndsAt = offerEndsAt;
		this.updatedAt = now;
	}

	public void setStatus(String status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}
}
