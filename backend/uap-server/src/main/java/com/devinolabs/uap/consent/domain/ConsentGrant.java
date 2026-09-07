package com.devinolabs.uap.consent.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Athlete-granted, team-scoped consent bound to a specific membership generation.
 *
 * <p>Lifecycle: {@code ACTIVE} → {@code REVOKED}. Re-grant creates a new grant id; never resurrect.
 */
public class ConsentGrant {

	private final ConsentGrantId id;
	private final UUID athleteId;
	private final UUID teamId;
	private final UUID organizationId;
	private final UUID teamMembershipId;
	private final Set<ConsentScope> scopes;
	private ConsentGrantStatus status;
	private final Instant createdAt;
	private Instant revokedAt;
	private Instant updatedAt;
	private long version;

	private ConsentGrant(
			ConsentGrantId id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes,
			ConsentGrantStatus status,
			Instant createdAt,
			Instant revokedAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "ConsentGrant id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
		this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		this.teamMembershipId = Objects.requireNonNull(teamMembershipId, "teamMembershipId must not be null");
		this.scopes = freezeScopes(scopes);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.revokedAt = revokedAt;
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		if (status == ConsentGrantStatus.REVOKED && revokedAt == null) {
			throw new IllegalArgumentException("REVOKED grant requires revokedAt");
		}
		if (status == ConsentGrantStatus.ACTIVE && revokedAt != null) {
			throw new IllegalArgumentException("ACTIVE grant must not have revokedAt");
		}
	}

	public static ConsentGrant grant(
			ConsentGrantId id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new ConsentGrant(
				id,
				athleteId,
				teamId,
				organizationId,
				teamMembershipId,
				scopes,
				ConsentGrantStatus.ACTIVE,
				now,
				null,
				now,
				0L);
	}

	public static ConsentGrant rehydrate(
			ConsentGrantId id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes,
			ConsentGrantStatus status,
			Instant createdAt,
			Instant revokedAt,
			Instant updatedAt,
			long version) {
		return new ConsentGrant(
				id,
				athleteId,
				teamId,
				organizationId,
				teamMembershipId,
				scopes,
				status,
				createdAt,
				revokedAt,
				updatedAt,
				version);
	}

	/**
	 * Transitions ACTIVE → REVOKED. Idempotent when already REVOKED.
	 *
	 * @return {@code true} when status changed; {@code false} when already revoked
	 */
	public boolean revoke(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == ConsentGrantStatus.REVOKED) {
			return false;
		}
		Instant now = Instant.now(clock);
		this.status = ConsentGrantStatus.REVOKED;
		this.revokedAt = now;
		this.updatedAt = now;
		return true;
	}

	public boolean isActive() {
		return status == ConsentGrantStatus.ACTIVE;
	}

	public boolean includesScope(ConsentScope scope) {
		Objects.requireNonNull(scope, "scope must not be null");
		return scopes.contains(scope);
	}

	private static Set<ConsentScope> freezeScopes(Set<ConsentScope> scopes) {
		Objects.requireNonNull(scopes, "scopes must not be null");
		if (scopes.isEmpty()) {
			throw new IllegalArgumentException("scopes must not be empty");
		}
		for (ConsentScope scope : scopes) {
			Objects.requireNonNull(scope, "scope must not be null");
		}
		return Collections.unmodifiableSet(EnumSet.copyOf(scopes));
	}

	public ConsentGrantId id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public UUID teamId() {
		return teamId;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public UUID teamMembershipId() {
		return teamMembershipId;
	}

	public Set<ConsentScope> scopes() {
		return scopes;
	}

	public ConsentGrantStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant revokedAt() {
		return revokedAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
