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

import com.devinolabs.uap.integrations.domain.DeviceLifecycle;
import com.devinolabs.uap.integrations.domain.DeviceType;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Entity
@Table(name = "integration_devices")
class IntegrationDeviceJpaEntity implements Persistable<UUID> {

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

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "connection_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID connectionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 40)
	private HealthProviderKey provider;

	@Column(name = "external_device_id", nullable = false, updatable = false, length = 191)
	private String externalDeviceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "device_type", nullable = false, length = 40)
	private DeviceType deviceType;

	@Column(name = "manufacturer", length = 80)
	private String manufacturer;

	@Column(name = "model", length = 80)
	private String model;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(name = "firmware_version", length = 64)
	private String firmwareVersion;

	@Column(name = "capabilities", nullable = false, length = 512)
	private String capabilities;

	@Enumerated(EnumType.STRING)
	@Column(name = "lifecycle_state", nullable = false, length = 30)
	private DeviceLifecycle lifecycleState;

	@Column(name = "first_seen_at", nullable = false, updatable = false)
	private Instant firstSeenAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "last_successful_data_at")
	private Instant lastSuccessfulDataAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationDeviceJpaEntity() {
	}

	IntegrationDeviceJpaEntity(
			UUID id,
			UUID athleteId,
			UUID accountId,
			UUID connectionId,
			HealthProviderKey provider,
			String externalDeviceId,
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			String capabilities,
			DeviceLifecycle lifecycleState,
			Instant firstSeenAt,
			Instant lastSeenAt,
			Instant lastSuccessfulDataAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean newlyPersisted) {
		this.id = id;
		this.athleteId = athleteId;
		this.accountId = accountId;
		this.connectionId = connectionId;
		this.provider = provider;
		this.externalDeviceId = externalDeviceId;
		this.deviceType = deviceType;
		this.manufacturer = manufacturer;
		this.model = model;
		this.displayName = displayName;
		this.firmwareVersion = firmwareVersion;
		this.capabilities = capabilities;
		this.lifecycleState = lifecycleState;
		this.firstSeenAt = firstSeenAt;
		this.lastSeenAt = lastSeenAt;
		this.lastSuccessfulDataAt = lastSuccessfulDataAt;
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

	UUID getConnectionId() {
		return connectionId;
	}

	HealthProviderKey getProvider() {
		return provider;
	}

	String getExternalDeviceId() {
		return externalDeviceId;
	}

	DeviceType getDeviceType() {
		return deviceType;
	}

	String getManufacturer() {
		return manufacturer;
	}

	String getModel() {
		return model;
	}

	String getDisplayName() {
		return displayName;
	}

	String getFirmwareVersion() {
		return firmwareVersion;
	}

	String getCapabilities() {
		return capabilities;
	}

	DeviceLifecycle getLifecycleState() {
		return lifecycleState;
	}

	Instant getFirstSeenAt() {
		return firstSeenAt;
	}

	Instant getLastSeenAt() {
		return lastSeenAt;
	}

	Instant getLastSuccessfulDataAt() {
		return lastSuccessfulDataAt;
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
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			String capabilities,
			DeviceLifecycle lifecycleState,
			Instant lastSeenAt,
			Instant lastSuccessfulDataAt,
			Instant updatedAt) {
		this.deviceType = deviceType;
		this.manufacturer = manufacturer;
		this.model = model;
		this.displayName = displayName;
		this.firmwareVersion = firmwareVersion;
		this.capabilities = capabilities;
		this.lifecycleState = lifecycleState;
		this.lastSeenAt = lastSeenAt;
		this.lastSuccessfulDataAt = lastSuccessfulDataAt;
		this.updatedAt = updatedAt;
	}
}
