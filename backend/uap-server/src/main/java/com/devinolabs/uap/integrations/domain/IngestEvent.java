package com.devinolabs.uap.integrations.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable integrations inbox receipt (ADR-051). Distinct from billing_provider_events.
 */
public class IngestEvent {

	public static final String EVENT_TYPE_EVIDENCE_BATCH = "EVIDENCE_BATCH";

	private final IngestEventId id;
	private final HealthProviderKey provider;
	private final String providerEventId;
	private final String eventType;
	private final ConnectionId connectionId;
	private final UUID athleteId;
	private final Instant receivedAt;
	private Instant processedAt;
	private IngestProcessingStatus processingStatus;
	private String errorCode;
	private SyncRunId syncRunId;

	private IngestEvent(
			IngestEventId id,
			HealthProviderKey provider,
			String providerEventId,
			String eventType,
			ConnectionId connectionId,
			UUID athleteId,
			Instant receivedAt,
			Instant processedAt,
			IngestProcessingStatus processingStatus,
			String errorCode,
			SyncRunId syncRunId) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.providerEventId = requireNonBlank(providerEventId, "providerEventId");
		if (this.providerEventId.length() > 191) {
			throw new IllegalArgumentException("providerEventId must be <= 191 characters");
		}
		this.eventType = requireNonBlank(eventType, "eventType");
		this.connectionId = connectionId;
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
		this.processedAt = processedAt;
		this.processingStatus = Objects.requireNonNull(processingStatus, "processingStatus must not be null");
		this.errorCode = normalizeNullable(errorCode);
		this.syncRunId = syncRunId;
	}

	public static IngestEvent beginReceived(
			IngestEventId id,
			HealthProviderKey provider,
			String providerEventId,
			String eventType,
			ConnectionId connectionId,
			UUID athleteId,
			Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		return new IngestEvent(
				id,
				provider,
				providerEventId,
				eventType,
				connectionId,
				athleteId,
				Instant.now(clock),
				null,
				IngestProcessingStatus.RECEIVED,
				null,
				null);
	}

	public static IngestEvent rehydrate(
			IngestEventId id,
			HealthProviderKey provider,
			String providerEventId,
			String eventType,
			ConnectionId connectionId,
			UUID athleteId,
			Instant receivedAt,
			Instant processedAt,
			IngestProcessingStatus processingStatus,
			String errorCode,
			SyncRunId syncRunId) {
		return new IngestEvent(
				id,
				provider,
				providerEventId,
				eventType,
				connectionId,
				athleteId,
				receivedAt,
				processedAt,
				processingStatus,
				errorCode,
				syncRunId);
	}

	public void markProcessed(SyncRunId syncRunId, Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (processingStatus != IngestProcessingStatus.RECEIVED
				&& processingStatus != IngestProcessingStatus.FAILED) {
			throw new IllegalStateException("Only RECEIVED/FAILED ingest events can complete; was " + processingStatus);
		}
		this.processingStatus = IngestProcessingStatus.PROCESSED;
		this.processedAt = Instant.now(clock);
		this.syncRunId = syncRunId;
		this.errorCode = null;
	}

	public void markFailed(String errorCode, Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (errorCode == null || errorCode.isBlank()) {
			throw new IllegalArgumentException("errorCode is required on failure");
		}
		this.processingStatus = IngestProcessingStatus.FAILED;
		this.processedAt = Instant.now(clock);
		this.errorCode = errorCode.trim();
	}

	public boolean isTerminalSuccess() {
		return processingStatus == IngestProcessingStatus.PROCESSED
				|| processingStatus == IngestProcessingStatus.IGNORED;
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

	public IngestEventId id() {
		return id;
	}

	public HealthProviderKey provider() {
		return provider;
	}

	public String providerEventId() {
		return providerEventId;
	}

	public String eventType() {
		return eventType;
	}

	public ConnectionId connectionId() {
		return connectionId;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public Instant receivedAt() {
		return receivedAt;
	}

	public Instant processedAt() {
		return processedAt;
	}

	public IngestProcessingStatus processingStatus() {
		return processingStatus;
	}

	public String errorCode() {
		return errorCode;
	}

	public SyncRunId syncRunId() {
		return syncRunId;
	}
}
