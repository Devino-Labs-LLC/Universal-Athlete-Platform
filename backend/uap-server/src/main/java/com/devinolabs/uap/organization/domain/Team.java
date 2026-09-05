package com.devinolabs.uap.organization.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class Team {

	private final TeamId id;
	private final OrganizationId organizationId;
	private String name;
	private TeamStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Team(
			TeamId id,
			OrganizationId organizationId,
			String name,
			TeamStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Team id must not be null");
		this.organizationId = Objects.requireNonNull(organizationId, "Team organizationId must not be null");
		this.name = OrganizationNames.requireDisplayName(name);
		this.status = Objects.requireNonNull(status, "Team status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "Team createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "Team updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static Team register(TeamId id, OrganizationId organizationId, String name) {
		return register(id, organizationId, name, Clock.systemUTC());
	}

	public static Team register(TeamId id, OrganizationId organizationId, String name, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Team(id, organizationId, name, TeamStatus.ACTIVE, now, now, 0L);
	}

	public static Team rehydrate(
			TeamId id,
			OrganizationId organizationId,
			String name,
			TeamStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Team(id, organizationId, name, status, createdAt, updatedAt, version);
	}

	public void rename(String name, Clock clock) {
		requireMutable(clock);
		this.name = OrganizationNames.requireDisplayName(name);
		this.updatedAt = Instant.now(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == TeamStatus.ARCHIVED) {
			throw new IllegalStateException("Team is already archived");
		}
		this.status = TeamStatus.ARCHIVED;
		this.updatedAt = Instant.now(clock);
	}

	private void requireMutable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == TeamStatus.ARCHIVED) {
			throw new IllegalStateException("Archived team cannot be modified");
		}
	}

	public TeamId id() {
		return id;
	}

	public OrganizationId organizationId() {
		return organizationId;
	}

	public String name() {
		return name;
	}

	public TeamStatus status() {
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
