package com.devinolabs.uap.integrations.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Athlete-owned provider/hub connection aggregate (ADR-048 / ADR-050).
 *
 * <p>Layer A (provider connect) and Layer B (process consent) are distinct: {@link #confirmConnected}
 * records both OS confirmation and process-consent B for V5 OS hubs. Disconnect drops credential
 * refs and stops future sync; historical evidence retention is out of this aggregate (D9 = B).
 */
public class Connection {

	private final ConnectionId id;
	private final UUID athleteId;
	private final UUID accountId;
	private final HealthProviderKey provider;
	private ConnectionStatus status;
	private boolean processConsentGranted;
	private Instant processConsentGrantedAt;
	private String scopesJson;
	private Instant connectedAt;
	private Instant disconnectedAt;
	private Instant lastSuccessfulSyncAt;
	private Instant lastAttemptedSyncAt;
	private String providerUserRef;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Connection(
			ConnectionId id,
			UUID athleteId,
			UUID accountId,
			HealthProviderKey provider,
			ConnectionStatus status,
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
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.processConsentGranted = processConsentGranted;
		this.processConsentGrantedAt = processConsentGrantedAt;
		this.scopesJson = scopesJson;
		this.connectedAt = connectedAt;
		this.disconnectedAt = disconnectedAt;
		this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
		this.lastAttemptedSyncAt = lastAttemptedSyncAt;
		this.providerUserRef = normalizeRef(providerUserRef);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static Connection beginPending(
			ConnectionId id,
			UUID athleteId,
			UUID accountId,
			HealthProviderKey provider,
			Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		Instant now = Instant.now(clock);
		return new Connection(
				id,
				athleteId,
				accountId,
				provider,
				ConnectionStatus.PENDING,
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static Connection rehydrate(
			ConnectionId id,
			UUID athleteId,
			UUID accountId,
			HealthProviderKey provider,
			ConnectionStatus status,
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
			long version) {
		return new Connection(
				id,
				athleteId,
				accountId,
				provider,
				status,
				processConsentGranted,
				processConsentGrantedAt,
				scopesJson,
				connectedAt,
				disconnectedAt,
				lastSuccessfulSyncAt,
				lastAttemptedSyncAt,
				providerUserRef,
				createdAt,
				updatedAt,
				version);
	}

	/** Reopens a disconnected row for the same athlete+provider (UNIQUE constraint). */
	public void reopenPending(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != ConnectionStatus.DISCONNECTED) {
			throw new IllegalStateException("Only DISCONNECTED connections can be reopened; was " + status);
		}
		Instant now = Instant.now(clock);
		this.status = ConnectionStatus.PENDING;
		this.processConsentGranted = false;
		this.processConsentGrantedAt = null;
		this.scopesJson = null;
		this.connectedAt = null;
		this.disconnectedAt = null;
		this.providerUserRef = null;
		this.updatedAt = now;
	}

	/**
	 * Athlete confirms OS permission granted. Records Layer B process consent for V5 OS hubs.
	 */
	public void confirmConnected(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != ConnectionStatus.PENDING) {
			throw new IllegalStateException("Only PENDING connections can be confirmed; was " + status);
		}
		Instant now = Instant.now(clock);
		this.status = ConnectionStatus.CONNECTED;
		this.processConsentGranted = true;
		this.processConsentGrantedAt = now;
		this.connectedAt = now;
		this.disconnectedAt = null;
		this.updatedAt = now;
	}

	public void disconnect(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status == ConnectionStatus.DISCONNECTED) {
			return;
		}
		Instant now = Instant.now(clock);
		this.status = ConnectionStatus.DISCONNECTED;
		this.disconnectedAt = now;
		this.providerUserRef = null;
		this.updatedAt = now;
	}

	public void markNeedsReauth(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != ConnectionStatus.CONNECTED && status != ConnectionStatus.NEEDS_REAUTH) {
			throw new IllegalStateException("NEEDS_REAUTH requires CONNECTED (or already NEEDS_REAUTH); was " + status);
		}
		this.status = ConnectionStatus.NEEDS_REAUTH;
		this.updatedAt = Instant.now(clock);
	}

	public void markError(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (status != ConnectionStatus.PENDING && status != ConnectionStatus.ERROR) {
			throw new IllegalStateException("ERROR is only reachable from PENDING (or already ERROR); was " + status);
		}
		this.status = ConnectionStatus.ERROR;
		this.updatedAt = Instant.now(clock);
	}

	public void recordSyncAttempt(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		assertAcceptsSync();
		Instant now = Instant.now(clock);
		this.lastAttemptedSyncAt = now;
		this.updatedAt = now;
	}

	public void recordSuccessfulSync(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		assertAcceptsSync();
		Instant now = Instant.now(clock);
		this.lastAttemptedSyncAt = now;
		this.lastSuccessfulSyncAt = now;
		this.updatedAt = now;
	}

	public boolean acceptsSync() {
		return status == ConnectionStatus.CONNECTED;
	}

	private void assertAcceptsSync() {
		if (!acceptsSync()) {
			throw new IllegalStateException("Sync is only accepted while CONNECTED; was " + status);
		}
	}

	public ConnectionId id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public UUID accountId() {
		return accountId;
	}

	public HealthProviderKey provider() {
		return provider;
	}

	public ConnectionStatus status() {
		return status;
	}

	public boolean processConsentGranted() {
		return processConsentGranted;
	}

	public Instant processConsentGrantedAt() {
		return processConsentGrantedAt;
	}

	public String scopesJson() {
		return scopesJson;
	}

	public Instant connectedAt() {
		return connectedAt;
	}

	public Instant disconnectedAt() {
		return disconnectedAt;
	}

	public Instant lastSuccessfulSyncAt() {
		return lastSuccessfulSyncAt;
	}

	public Instant lastAttemptedSyncAt() {
		return lastAttemptedSyncAt;
	}

	public String providerUserRef() {
		return providerUserRef;
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

	private static String normalizeRef(String ref) {
		if (ref == null) {
			return null;
		}
		String trimmed = ref.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
