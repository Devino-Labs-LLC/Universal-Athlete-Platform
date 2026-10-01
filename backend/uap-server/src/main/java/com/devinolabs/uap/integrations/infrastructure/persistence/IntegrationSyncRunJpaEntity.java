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
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.integrations.domain.SyncRunStatus;

@Entity
@Table(name = "integration_sync_runs")
class IntegrationSyncRunJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "connection_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID connectionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SyncRunStatus status;

	@Column(name = "requested_at", nullable = false, updatable = false)
	private Instant requestedAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "records_accepted", nullable = false)
	private int recordsAccepted;

	@Column(name = "records_rejected", nullable = false)
	private int recordsRejected;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationSyncRunJpaEntity() {
	}

	IntegrationSyncRunJpaEntity(
			UUID id,
			UUID connectionId,
			UUID athleteId,
			SyncRunStatus status,
			Instant requestedAt,
			Instant startedAt,
			Instant finishedAt,
			String errorCode,
			int recordsAccepted,
			int recordsRejected,
			long version,
			boolean newlyPersisted) {
		this.id = id;
		this.connectionId = connectionId;
		this.athleteId = athleteId;
		this.status = status;
		this.requestedAt = requestedAt;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.errorCode = errorCode;
		this.recordsAccepted = recordsAccepted;
		this.recordsRejected = recordsRejected;
		this.version = version;
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

	UUID getConnectionId() {
		return connectionId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	SyncRunStatus getStatus() {
		return status;
	}

	Instant getRequestedAt() {
		return requestedAt;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getFinishedAt() {
		return finishedAt;
	}

	String getErrorCode() {
		return errorCode;
	}

	int getRecordsAccepted() {
		return recordsAccepted;
	}

	int getRecordsRejected() {
		return recordsRejected;
	}

	long getVersion() {
		return version;
	}

	void applyDomainState(
			SyncRunStatus status,
			Instant startedAt,
			Instant finishedAt,
			String errorCode,
			int recordsAccepted,
			int recordsRejected) {
		this.status = status;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.errorCode = errorCode;
		this.recordsAccepted = recordsAccepted;
		this.recordsRejected = recordsRejected;
	}

}
