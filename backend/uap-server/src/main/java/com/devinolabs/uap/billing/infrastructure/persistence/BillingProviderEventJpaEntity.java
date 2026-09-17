package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;

@Entity
@Table(name = "billing_provider_events")
class BillingProviderEventJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 30)
	private BillingProvider provider;

	@Column(name = "provider_event_id", nullable = false, updatable = false, length = 255)
	private String providerEventId;

	@Column(name = "event_type", nullable = false, updatable = false, length = 120)
	private String eventType;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", nullable = false, length = 20)
	private ProviderEventProcessingStatus processingStatus;

	@Transient
	private boolean newlyPersisted = true;

	protected BillingProviderEventJpaEntity() {
	}

	BillingProviderEventJpaEntity(
			UUID id,
			BillingProvider provider,
			String providerEventId,
			String eventType,
			Instant receivedAt,
			Instant processedAt,
			ProviderEventProcessingStatus processingStatus,
			boolean newlyPersisted) {
		this.id = id;
		this.provider = provider;
		this.providerEventId = providerEventId;
		this.eventType = eventType;
		this.receivedAt = receivedAt;
		this.processedAt = processedAt;
		this.processingStatus = processingStatus;
		this.newlyPersisted = newlyPersisted;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return newlyPersisted;
	}

	@PostLoad
	@PostPersist
	void markLoaded() {
		this.newlyPersisted = false;
	}

	BillingProvider getProvider() {
		return provider;
	}

	String getProviderEventId() {
		return providerEventId;
	}

	String getEventType() {
		return eventType;
	}

	Instant getReceivedAt() {
		return receivedAt;
	}

	Instant getProcessedAt() {
		return processedAt;
	}

	ProviderEventProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	void complete(ProviderEventProcessingStatus status, Instant processedAt) {
		this.processingStatus = status;
		this.processedAt = processedAt;
	}

}
