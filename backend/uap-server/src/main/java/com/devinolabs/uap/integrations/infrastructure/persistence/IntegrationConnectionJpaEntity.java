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

import com.devinolabs.uap.integrations.domain.ConnectionStatus;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Entity
@Table(name = "integration_connections")
class IntegrationConnectionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 40)
	private HealthProviderKey provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "lifecycle_state", nullable = false, length = 30)
	private ConnectionStatus lifecycleState;

	@Column(name = "process_consent_granted", nullable = false)
	private boolean processConsentGranted;

	@Column(name = "process_consent_granted_at")
	private Instant processConsentGrantedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "scopes", columnDefinition = "json")
	private String scopesJson;

	@Column(name = "connected_at")
	private Instant connectedAt;

	@Column(name = "disconnected_at")
	private Instant disconnectedAt;

	@Column(name = "last_successful_sync_at")
	private Instant lastSuccessfulSyncAt;

	@Column(name = "last_attempted_sync_at")
	private Instant lastAttemptedSyncAt;

	@Column(name = "provider_user_ref", length = 255)
	private String providerUserRef;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationConnectionJpaEntity() {
	}

	IntegrationConnectionJpaEntity(
			UUID id,
			UUID athleteId,
			UUID accountId,
			HealthProviderKey provider,
			ConnectionStatus lifecycleState,
			boolean processConsentGranted,
			Instant processConsentGrantedAt,
			String scopesJson,
			Instant connectedAt,
			Instant disconnectedAt,
			Instant lastSuccessfulSyncAt,
			Instant lastAttemptedSyncAt,
			String providerUserRef,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean newlyPersisted) {
		this.id = id;
		this.athleteId = athleteId;
		this.accountId = accountId;
		this.provider = provider;
		this.lifecycleState = lifecycleState;
		this.processConsentGranted = processConsentGranted;
		this.processConsentGrantedAt = processConsentGrantedAt;
		this.scopesJson = scopesJson;
		this.connectedAt = connectedAt;
		this.disconnectedAt = disconnectedAt;
		this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
		this.lastAttemptedSyncAt = lastAttemptedSyncAt;
		this.providerUserRef = providerUserRef;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
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

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getAccountId() {
		return accountId;
	}

	HealthProviderKey getProvider() {
		return provider;
	}

	ConnectionStatus getLifecycleState() {
		return lifecycleState;
	}

	boolean isProcessConsentGranted() {
		return processConsentGranted;
	}

	Instant getProcessConsentGrantedAt() {
		return processConsentGrantedAt;
	}

	String getScopesJson() {
		return scopesJson;
	}

	Instant getConnectedAt() {
		return connectedAt;
	}

	Instant getDisconnectedAt() {
		return disconnectedAt;
	}

	Instant getLastSuccessfulSyncAt() {
		return lastSuccessfulSyncAt;
	}

	Instant getLastAttemptedSyncAt() {
		return lastAttemptedSyncAt;
	}

	String getProviderUserRef() {
		return providerUserRef;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

	void applyDomainState(
			ConnectionStatus lifecycleState,
			boolean processConsentGranted,
			Instant processConsentGrantedAt,
			String scopesJson,
			Instant connectedAt,
			Instant disconnectedAt,
			Instant lastSuccessfulSyncAt,
			Instant lastAttemptedSyncAt,
			String providerUserRef,
			Instant updatedAt) {
		this.lifecycleState = lifecycleState;
		this.processConsentGranted = processConsentGranted;
		this.processConsentGrantedAt = processConsentGrantedAt;
		this.scopesJson = scopesJson;
		this.connectedAt = connectedAt;
		this.disconnectedAt = disconnectedAt;
		this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
		this.lastAttemptedSyncAt = lastAttemptedSyncAt;
		this.providerUserRef = providerUserRef;
		this.updatedAt = updatedAt;
	}

}
