package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.math.BigDecimal;
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

import com.devinolabs.uap.integrations.domain.EvidenceStatus;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.ProvenanceClass;
import com.devinolabs.uap.integrations.domain.SignalFamily;

@Entity
@Table(name = "integration_evidence")
class IntegrationEvidenceJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "connection_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID connectionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 40)
	private HealthProviderKey provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "signal_family", nullable = false, length = 20)
	private SignalFamily signalFamily;

	@Column(name = "signal_type", nullable = false, length = 80)
	private String signalType;

	@Column(name = "external_record_id", nullable = false, updatable = false, length = 191)
	private String externalRecordId;

	@Column(name = "value_numeric", precision = 18, scale = 6)
	private BigDecimal valueNumeric;

	@Column(name = "value_text", length = 512)
	private String valueText;

	@Column(name = "unit_code", nullable = false, length = 32)
	private String unitCode;

	@Column(name = "period_start")
	private Instant periodStart;

	@Column(name = "period_end")
	private Instant periodEnd;

	@Column(name = "observed_at", nullable = false)
	private Instant observedAt;

	@Column(name = "provider_updated_at")
	private Instant providerUpdatedAt;

	@Column(name = "ingested_at", nullable = false)
	private Instant ingestedAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "sync_run_id", columnDefinition = "BINARY(16)")
	private UUID syncRunId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provenance_class", nullable = false, length = 30)
	private ProvenanceClass provenanceClass;

	@Column(name = "source_device_or_app", length = 128)
	private String sourceDeviceOrApp;

	@Column(name = "quality_code", length = 64)
	private String qualityCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EvidenceStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

	protected IntegrationEvidenceJpaEntity() {
	}

	IntegrationEvidenceJpaEntity(
			UUID id,
			UUID athleteId,
			UUID connectionId,
			HealthProviderKey provider,
			SignalFamily signalFamily,
			String signalType,
			String externalRecordId,
			BigDecimal valueNumeric,
			String valueText,
			String unitCode,
			Instant periodStart,
			Instant periodEnd,
			Instant observedAt,
			Instant providerUpdatedAt,
			Instant ingestedAt,
			UUID syncRunId,
			ProvenanceClass provenanceClass,
			String sourceDeviceOrApp,
			String qualityCode,
			EvidenceStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean newlyPersisted) {
		this.id = id;
		this.athleteId = athleteId;
		this.connectionId = connectionId;
		this.provider = provider;
		this.signalFamily = signalFamily;
		this.signalType = signalType;
		this.externalRecordId = externalRecordId;
		this.valueNumeric = valueNumeric;
		this.valueText = valueText;
		this.unitCode = unitCode;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.observedAt = observedAt;
		this.providerUpdatedAt = providerUpdatedAt;
		this.ingestedAt = ingestedAt;
		this.syncRunId = syncRunId;
		this.provenanceClass = provenanceClass;
		this.sourceDeviceOrApp = sourceDeviceOrApp;
		this.qualityCode = qualityCode;
		this.status = status;
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

	UUID getConnectionId() {
		return connectionId;
	}

	HealthProviderKey getProvider() {
		return provider;
	}

	SignalFamily getSignalFamily() {
		return signalFamily;
	}

	String getSignalType() {
		return signalType;
	}

	String getExternalRecordId() {
		return externalRecordId;
	}

	BigDecimal getValueNumeric() {
		return valueNumeric;
	}

	String getValueText() {
		return valueText;
	}

	String getUnitCode() {
		return unitCode;
	}

	Instant getPeriodStart() {
		return periodStart;
	}

	Instant getPeriodEnd() {
		return periodEnd;
	}

	Instant getObservedAt() {
		return observedAt;
	}

	Instant getProviderUpdatedAt() {
		return providerUpdatedAt;
	}

	Instant getIngestedAt() {
		return ingestedAt;
	}

	UUID getSyncRunId() {
		return syncRunId;
	}

	ProvenanceClass getProvenanceClass() {
		return provenanceClass;
	}

	String getSourceDeviceOrApp() {
		return sourceDeviceOrApp;
	}

	String getQualityCode() {
		return qualityCode;
	}

	EvidenceStatus getStatus() {
		return status;
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
			SignalFamily signalFamily,
			String signalType,
			BigDecimal valueNumeric,
			String valueText,
			String unitCode,
			Instant periodStart,
			Instant periodEnd,
			Instant observedAt,
			Instant providerUpdatedAt,
			Instant ingestedAt,
			UUID syncRunId,
			ProvenanceClass provenanceClass,
			String sourceDeviceOrApp,
			String qualityCode,
			EvidenceStatus status,
			Instant updatedAt) {
		this.signalFamily = signalFamily;
		this.signalType = signalType;
		this.valueNumeric = valueNumeric;
		this.valueText = valueText;
		this.unitCode = unitCode;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.observedAt = observedAt;
		this.providerUpdatedAt = providerUpdatedAt;
		this.ingestedAt = ingestedAt;
		this.syncRunId = syncRunId;
		this.provenanceClass = provenanceClass;
		this.sourceDeviceOrApp = sourceDeviceOrApp;
		this.qualityCode = qualityCode;
		this.status = status;
		this.updatedAt = updatedAt;
	}
}
