package com.devinolabs.uap.integrations.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Single sync attempt for a connection (ADR-048 / ADR-049).
 *
 * <p>OS hubs (HealthKit / Health Connect) are upload-only — pull sync fails with
 * {@code OS_HUB_UPLOAD_ONLY}. Future OAuth pull providers without an adapter fail with
 * {@code NO_ADAPTER}. Never fake SUCCEEDED.
 */
public class SyncRun {

	private final SyncRunId id;
	private final ConnectionId connectionId;
	private final UUID athleteId;
	private SyncRunStatus status;
	private final Instant requestedAt;
	private Instant startedAt;
	private Instant finishedAt;
	private String errorCode;
	private int recordsAccepted;
	private int recordsRejected;
	private long version;

	private SyncRun(
			SyncRunId id,
			ConnectionId connectionId,
			UUID athleteId,
			SyncRunStatus status,
			Instant requestedAt,
			Instant startedAt,
			Instant finishedAt,
			String errorCode,
			int recordsAccepted,
			int recordsRejected,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.errorCode = errorCode;
		if (recordsAccepted < 0 || recordsRejected < 0) {
			throw new IllegalArgumentException("Record counts must not be negative");
		}
		this.recordsAccepted = recordsAccepted;
		this.recordsRejected = recordsRejected;
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static SyncRun request(
			SyncRunId id,
			ConnectionId connectionId,
			UUID athleteId,
			Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		Instant now = Instant.now(clock);
		return new SyncRun(
				id,
				connectionId,
				athleteId,
				SyncRunStatus.REQUESTED,
				now,
				null,
				null,
				null,
				0,
				0,
				0L);
	}

	public static SyncRun rehydrate(
			SyncRunId id,
			ConnectionId connectionId,
			UUID athleteId,
			SyncRunStatus status,
			Instant requestedAt,
			Instant startedAt,
			Instant finishedAt,
			String errorCode,
			int recordsAccepted,
			int recordsRejected,
			long version) {
		return new SyncRun(
				id,
				connectionId,
				athleteId,
				status,
				requestedAt,
				startedAt,
				finishedAt,
				errorCode,
				recordsAccepted,
				recordsRejected,
				version);
	}

	public void markRunning(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != SyncRunStatus.REQUESTED) {
			throw new IllegalStateException("Only REQUESTED sync runs can start; was " + status);
		}
		this.status = SyncRunStatus.RUNNING;
		this.startedAt = Instant.now(clock);
	}

	/** Adapter-backed success with zero records accepted (empty window). */
	public void completeSucceededEmpty(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != SyncRunStatus.RUNNING) {
			throw new IllegalStateException("Only RUNNING sync runs can succeed; was " + status);
		}
		Instant now = Instant.now(clock);
		this.status = SyncRunStatus.SUCCEEDED;
		this.finishedAt = now;
		this.recordsAccepted = 0;
		this.recordsRejected = 0;
		this.errorCode = null;
	}

	/**
	 * OS hubs are client upload-only (evidence-batch). Server pull is not supported →
	 * REQUESTED → RUNNING → FAILED({@code OS_HUB_UPLOAD_ONLY}).
	 */
	public void completeOsHubUploadOnlyPipeline(Clock clock) {
		markRunning(clock);
		fail("OS_HUB_UPLOAD_ONLY", clock);
	}

	/**
	 * Honesty for future pull providers: no adapter registered →
	 * REQUESTED → RUNNING → FAILED({@code NO_ADAPTER}).
	 */
	public void completeNoAdapterPipeline(Clock clock) {
		markRunning(clock);
		fail("NO_ADAPTER", clock);
	}

	/**
	 * Evidence-batch upload outcome (F2). Does not mutate readiness — counts only.
	 * Zero rejects → SUCCEEDED; any rejects with accepts → PARTIAL; all rejects → PARTIAL.
	 */
	public void completeUploadBatch(int accepted, int rejected, Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (accepted < 0 || rejected < 0) {
			throw new IllegalArgumentException("Record counts must not be negative");
		}
		if (status == SyncRunStatus.REQUESTED) {
			markRunning(clock);
		}
		if (status != SyncRunStatus.RUNNING) {
			throw new IllegalStateException("Only RUNNING sync runs can complete an upload batch; was " + status);
		}
		Instant now = Instant.now(clock);
		this.recordsAccepted = accepted;
		this.recordsRejected = rejected;
		this.finishedAt = now;
		this.errorCode = null;
		this.status = rejected == 0 ? SyncRunStatus.SUCCEEDED : SyncRunStatus.PARTIAL;
	}

	public void fail(String errorCode, Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != SyncRunStatus.RUNNING && status != SyncRunStatus.REQUESTED) {
			throw new IllegalStateException("Only REQUESTED/RUNNING sync runs can fail; was " + status);
		}
		if (errorCode == null || errorCode.isBlank()) {
			throw new IllegalArgumentException("errorCode is required on failure");
		}
		Instant now = Instant.now(clock);
		if (this.startedAt == null) {
			this.startedAt = now;
		}
		this.status = SyncRunStatus.FAILED;
		this.finishedAt = now;
		this.errorCode = errorCode.trim();
	}

	public SyncRunId id() {
		return id;
	}

	public ConnectionId connectionId() {
		return connectionId;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public SyncRunStatus status() {
		return status;
	}

	public Instant requestedAt() {
		return requestedAt;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant finishedAt() {
		return finishedAt;
	}

	public String errorCode() {
		return errorCode;
	}

	public int recordsAccepted() {
		return recordsAccepted;
	}

	public int recordsRejected() {
		return recordsRejected;
	}

	public long version() {
		return version;
	}

}
