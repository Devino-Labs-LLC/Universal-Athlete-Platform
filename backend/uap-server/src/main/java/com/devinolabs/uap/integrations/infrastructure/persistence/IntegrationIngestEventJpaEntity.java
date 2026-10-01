package com.devinolabs.uap.integrations.infrastructure.persistence;

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

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.IngestProcessingStatus;

@Entity
@Table(name = "integration_ingest_events")
class IntegrationIngestEventJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 40)
	private HealthProviderKey provider;

	@Column(name = "provider_event_id", nullable = false, updatable = false, length = 191)
	private String providerEventId;

	@Column(name = "event_type", nullable = false, updatable = false, length = 64)
	private String eventType;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "connection_id", columnDefinition = "BINARY(16)")
	private UUID connectionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", nullable = false, length = 20)
	private IngestProcessingStatus processingStatus;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "sync_run_id", columnDefinition = "BINARY(16)")
	private UUID syncRunId;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationIngestEventJpaEntity() {
	}

	IntegrationIngestEventJpaEntity(
			UUID id,
			HealthProviderKey provider,
			String providerEventId,
			String eventType,
			UUID connectionId,
			UUID athleteId,
			Instant receivedAt,
			Instant processedAt,
			IngestProcessingStatus processingStatus,
			String errorCode,
			UUID syncRunId,
			boolean newlyPersisted) {
		this.id = id;
		this.provider = provider;
		this.providerEventId = providerEventId;
		this.eventType = eventType;
		this.connectionId = connectionId;
		this.athleteId = athleteId;
		this.receivedAt = receivedAt;
		this.processedAt = processedAt;
		this.processingStatus = processingStatus;
		this.errorCode = errorCode;
		this.syncRunId = syncRunId;
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

	HealthProviderKey getProvider() {
		return provider;
	}

	String getProviderEventId() {
		return providerEventId;
	}

	String getEventType() {
		return eventType;
	}

	UUID getConnectionId() {
		return connectionId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	Instant getReceivedAt() {
		return receivedAt;
	}

	Instant getProcessedAt() {
		return processedAt;
	}

	IngestProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	String getErrorCode() {
		return errorCode;
	}

	UUID getSyncRunId() {
		return syncRunId;
	}

	void applyDomainState(
			Instant processedAt,
			IngestProcessingStatus processingStatus,
			String errorCode,
			UUID syncRunId) {
		this.processedAt = processedAt;
		this.processingStatus = processingStatus;
		this.errorCode = errorCode;
		this.syncRunId = syncRunId;
	}
}
