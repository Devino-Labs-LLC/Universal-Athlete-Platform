package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Sync checkpoint persistence for F2+ adapters. Created in V38; unused by F1 HTTP.
 */
@Entity
@Table(name = "integration_sync_checkpoints")
class IntegrationSyncCheckpointJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "connection_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID connectionId;

	@Column(name = "stream_key", nullable = false, updatable = false, length = 80)
	private String streamKey;

	@Column(name = "cursor_type", nullable = false, length = 40)
	private String cursorType;

	@Column(name = "cursor_value", nullable = false, length = 512)
	private String cursorValue;

	@Column(name = "watermark_at")
	private Instant watermarkAt;

	@Column(name = "last_attempt_at")
	private Instant lastAttemptAt;

	@Column(name = "last_success_at")
	private Instant lastSuccessAt;

	@Column(name = "last_error_code", length = 64)
	private String lastErrorCode;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationSyncCheckpointJpaEntity() {
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

}
