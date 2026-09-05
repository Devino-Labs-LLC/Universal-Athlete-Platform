package com.devinolabs.uap.organization.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class Organization {

	private final OrganizationId id;
	private String name;
	private OrganizationStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Organization(
			OrganizationId id,
			String name,
			OrganizationStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Organization id must not be null");
		this.name = OrganizationNames.requireDisplayName(name);
		this.status = Objects.requireNonNull(status, "Organization status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "Organization createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "Organization updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static Organization register(OrganizationId id, String name) {
		return register(id, name, Clock.systemUTC());
	}

	public static Organization register(OrganizationId id, String name, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Organization(id, name, OrganizationStatus.ACTIVE, now, now, 0L);
	}

	public static Organization rehydrate(
			OrganizationId id,
			String name,
			OrganizationStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Organization(id, name, status, createdAt, updatedAt, version);
	}

	public void rename(String name, Clock clock) {
		requireMutable(clock);
		this.name = OrganizationNames.requireDisplayName(name);
		this.updatedAt = Instant.now(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == OrganizationStatus.ARCHIVED) {
			throw new IllegalStateException("Organization is already archived");
		}
		this.status = OrganizationStatus.ARCHIVED;
		this.updatedAt = Instant.now(clock);
	}

	private void requireMutable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == OrganizationStatus.ARCHIVED) {
			throw new IllegalStateException("Archived organization cannot be modified");
		}
	}

	public OrganizationId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public OrganizationStatus status() {
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
