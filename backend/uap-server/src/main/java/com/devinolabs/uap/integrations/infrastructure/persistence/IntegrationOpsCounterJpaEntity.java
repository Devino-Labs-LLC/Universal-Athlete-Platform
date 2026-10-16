package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Entity
@Table(name = "integration_ops_counters")
class IntegrationOpsCounterJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 40)
	private HealthProviderKey provider;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "sync_attempts", nullable = false)
	private int syncAttempts;

	@Column(name = "sync_successes", nullable = false)
	private int syncSuccesses;

	@Column(name = "sync_failures", nullable = false)
	private int syncFailures;

	@Column(name = "evidence_accepted", nullable = false)
	private int evidenceAccepted;

	@Column(name = "evidence_rejected", nullable = false)
	private int evidenceRejected;

	@Column(name = "duplicates", nullable = false)
	private int duplicates;

	@Column(name = "adapter_errors", nullable = false)
	private int adapterErrors;

	@Column(name = "error_category", length = 64)
	private String errorCategory;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IntegrationOpsCounterJpaEntity() {
	}

	IntegrationOpsCounterJpaEntity(
			UUID id,
			HealthProviderKey provider,
			Instant windowStart,
			int syncAttempts,
			int syncSuccesses,
			int syncFailures,
			int evidenceAccepted,
			int evidenceRejected,
			int duplicates,
			int adapterErrors,
			String errorCategory,
			Instant updatedAt) {
		this.id = id;
		this.provider = provider;
		this.windowStart = windowStart;
		this.syncAttempts = syncAttempts;
		this.syncSuccesses = syncSuccesses;
		this.syncFailures = syncFailures;
		this.evidenceAccepted = evidenceAccepted;
		this.evidenceRejected = evidenceRejected;
		this.duplicates = duplicates;
		this.adapterErrors = adapterErrors;
		this.errorCategory = errorCategory;
		this.updatedAt = updatedAt;
	}

	UUID getId() {
		return id;
	}

	HealthProviderKey getProvider() {
		return provider;
	}

	Instant getWindowStart() {
		return windowStart;
	}

	int getSyncAttempts() {
		return syncAttempts;
	}

	int getSyncSuccesses() {
		return syncSuccesses;
	}

	int getSyncFailures() {
		return syncFailures;
	}

	int getEvidenceAccepted() {
		return evidenceAccepted;
	}

	int getEvidenceRejected() {
		return evidenceRejected;
	}

	int getDuplicates() {
		return duplicates;
	}

	int getAdapterErrors() {
		return adapterErrors;
	}

	String getErrorCategory() {
		return errorCategory;
	}

	void add(
			int syncAttempts,
			int syncSuccesses,
			int syncFailures,
			int evidenceAccepted,
			int evidenceRejected,
			int duplicates,
			int adapterErrors,
			String errorCategory,
			Instant updatedAt) {
		this.syncAttempts += syncAttempts;
		this.syncSuccesses += syncSuccesses;
		this.syncFailures += syncFailures;
		this.evidenceAccepted += evidenceAccepted;
		this.evidenceRejected += evidenceRejected;
		this.duplicates += duplicates;
		this.adapterErrors += adapterErrors;
		if (errorCategory != null && !errorCategory.isBlank()) {
			this.errorCategory = errorCategory;
		}
		this.updatedAt = updatedAt;
	}
}
