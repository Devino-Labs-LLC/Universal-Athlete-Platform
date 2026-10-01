package com.devinolabs.uap.integrations.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Provider-neutral connected observation (ADR-047). Parallel to athlete-entered check-ins;
 * never mutates readiness on its own.
 */
public class ConnectedEvidence {

	/** Client clocks may lead server clock by this skew before FUTURE_TIMESTAMP reject. */
	public static final Duration FUTURE_SKEW = Duration.ofMinutes(5);

	private final EvidenceId id;
	private final UUID athleteId;
	private final ConnectionId connectionId;
	private final HealthProviderKey provider;
	private SignalFamily signalFamily;
	private String signalType;
	private final String externalRecordId;
	private BigDecimal valueNumeric;
	private String valueText;
	private String unitCode;
	private Instant periodStart;
	private Instant periodEnd;
	private Instant observedAt;
	private Instant providerUpdatedAt;
	private Instant ingestedAt;
	private SyncRunId syncRunId;
	private ProvenanceClass provenanceClass;
	private String sourceDeviceOrApp;
	private String qualityCode;
	private EvidenceStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private ConnectedEvidence(
			EvidenceId id,
			UUID athleteId,
			ConnectionId connectionId,
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
			SyncRunId syncRunId,
			ProvenanceClass provenanceClass,
			String sourceDeviceOrApp,
			String qualityCode,
			EvidenceStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.signalFamily = Objects.requireNonNull(signalFamily, "signalFamily must not be null");
		this.signalType = requireNonBlank(signalType, "signalType");
		this.externalRecordId = requireNonBlank(externalRecordId, "externalRecordId");
		if (this.externalRecordId.length() > 191) {
			throw new IllegalArgumentException("externalRecordId must be <= 191 characters");
		}
		this.valueNumeric = valueNumeric;
		this.valueText = normalizeNullable(valueText);
		this.unitCode = EvidenceUnitCodes.normalizeAndRequire(unitCode);
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
		this.providerUpdatedAt = providerUpdatedAt;
		this.ingestedAt = Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");
		this.syncRunId = syncRunId;
		this.provenanceClass = Objects.requireNonNull(provenanceClass, "provenanceClass must not be null");
		this.sourceDeviceOrApp = normalizeNullable(sourceDeviceOrApp);
		this.qualityCode = normalizeNullable(qualityCode);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		assertPeriodConsistent();
		assertNonNegativeNumeric();
	}

	public static ConnectedEvidence create(
			EvidenceId id,
			UUID athleteId,
			ConnectionId connectionId,
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
			SyncRunId syncRunId,
			ProvenanceClass provenanceClass,
			String sourceDeviceOrApp,
			String qualityCode,
			Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		Instant now = Instant.now(clock);
		return new ConnectedEvidence(
				id,
				athleteId,
				connectionId,
				provider,
				signalFamily,
				signalType,
				externalRecordId,
				valueNumeric,
				valueText,
				unitCode,
				periodStart,
				periodEnd,
				observedAt,
				providerUpdatedAt,
				now,
				syncRunId,
				provenanceClass,
				sourceDeviceOrApp,
				qualityCode,
				EvidenceStatus.ACTIVE,
				now,
				now,
				0L);
	}

	public static ConnectedEvidence rehydrate(
			EvidenceId id,
			UUID athleteId,
			ConnectionId connectionId,
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
			SyncRunId syncRunId,
			ProvenanceClass provenanceClass,
			String sourceDeviceOrApp,
			String qualityCode,
			EvidenceStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new ConnectedEvidence(
				id,
				athleteId,
				connectionId,
				provider,
				signalFamily,
				signalType,
				externalRecordId,
				valueNumeric,
				valueText,
				unitCode,
				periodStart,
				periodEnd,
				observedAt,
				providerUpdatedAt,
				ingestedAt,
				syncRunId,
				provenanceClass,
				sourceDeviceOrApp,
				qualityCode,
				status,
				createdAt,
				updatedAt,
				version);
	}

	/**
	 * Idempotent upsert decision against an existing row with the same uniqueness key.
	 * Newer {@code providerUpdatedAt} replaces; equal timestamps rewrite payload; older is ignored.
	 * When both timestamps are null, rewrite (idempotent replay without provider clocks).
	 */
	public EvidenceUpsertOutcome applyIncoming(ConnectedEvidence incoming, Clock clock) {
		Objects.requireNonNull(incoming, "incoming must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		assertSameUniquenessKey(incoming);

		Instant existingTs = this.providerUpdatedAt;
		Instant incomingTs = incoming.providerUpdatedAt;

		if (existingTs != null && incomingTs != null && incomingTs.isBefore(existingTs)) {
			return EvidenceUpsertOutcome.IGNORED_STALE;
		}
		if (existingTs != null && incomingTs == null) {
			return EvidenceUpsertOutcome.IGNORED_STALE;
		}

		Instant now = Instant.now(clock);
		this.signalFamily = incoming.signalFamily;
		this.signalType = incoming.signalType;
		this.valueNumeric = incoming.valueNumeric;
		this.valueText = incoming.valueText;
		this.unitCode = incoming.unitCode;
		this.periodStart = incoming.periodStart;
		this.periodEnd = incoming.periodEnd;
		this.observedAt = incoming.observedAt;
		this.providerUpdatedAt = incoming.providerUpdatedAt;
		this.ingestedAt = now;
		this.syncRunId = incoming.syncRunId;
		this.provenanceClass = incoming.provenanceClass;
		this.sourceDeviceOrApp = incoming.sourceDeviceOrApp;
		this.qualityCode = incoming.qualityCode;
		this.status = EvidenceStatus.ACTIVE;
		this.updatedAt = now;
		assertPeriodConsistent();
		assertNonNegativeNumeric();
		return EvidenceUpsertOutcome.UPDATED;
	}

	public static void assertAcceptableObservationTime(
			Instant observedAt,
			Instant providerUpdatedAt,
			Clock clock,
			int backfillDays) {
		Objects.requireNonNull(observedAt, "observedAt must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		if (backfillDays < 1) {
			throw new IllegalArgumentException("backfillDays must be >= 1");
		}
		Instant now = Instant.now(clock);
		Instant futureLimit = now.plus(FUTURE_SKEW);
		if (observedAt.isAfter(futureLimit)) {
			throw new IllegalArgumentException("observedAt is too far in the future");
		}
		if (providerUpdatedAt != null && providerUpdatedAt.isAfter(futureLimit)) {
			throw new IllegalArgumentException("providerUpdatedAt is too far in the future");
		}
		Instant earliest = now.minus(Duration.ofDays(backfillDays));
		if (observedAt.isBefore(earliest)) {
			throw new IllegalArgumentException(
					"observedAt is older than the configured backfill window of " + backfillDays + " days");
		}
	}

	private void assertSameUniquenessKey(ConnectedEvidence incoming) {
		if (this.provider != incoming.provider
				|| !this.athleteId.equals(incoming.athleteId)
				|| !this.externalRecordId.equals(incoming.externalRecordId)) {
			throw new IllegalArgumentException("Incoming evidence uniqueness key does not match existing row");
		}
	}

	private void assertPeriodConsistent() {
		if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
			throw new IllegalArgumentException("periodEnd must not be before periodStart");
		}
	}

	private void assertNonNegativeNumeric() {
		if (valueNumeric != null && valueNumeric.signum() < 0) {
			throw new IllegalArgumentException("valueNumeric must not be negative");
		}
	}

	private static String requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value.trim();
	}

	private static String normalizeNullable(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public EvidenceId id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public ConnectionId connectionId() {
		return connectionId;
	}

	public HealthProviderKey provider() {
		return provider;
	}

	public SignalFamily signalFamily() {
		return signalFamily;
	}

	public String signalType() {
		return signalType;
	}

	public String externalRecordId() {
		return externalRecordId;
	}

	public BigDecimal valueNumeric() {
		return valueNumeric;
	}

	public String valueText() {
		return valueText;
	}

	public String unitCode() {
		return unitCode;
	}

	public Instant periodStart() {
		return periodStart;
	}

	public Instant periodEnd() {
		return periodEnd;
	}

	public Instant observedAt() {
		return observedAt;
	}

	public Instant providerUpdatedAt() {
		return providerUpdatedAt;
	}

	public Instant ingestedAt() {
		return ingestedAt;
	}

	public SyncRunId syncRunId() {
		return syncRunId;
	}

	public ProvenanceClass provenanceClass() {
		return provenanceClass;
	}

	public String sourceDeviceOrApp() {
		return sourceDeviceOrApp;
	}

	public String qualityCode() {
		return qualityCode;
	}

	public EvidenceStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}
}
