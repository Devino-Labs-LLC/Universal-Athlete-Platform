package com.devinolabs.uap.organization.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Organization-scoped membership. Slice A bootstraps ORG_OWNER only (no invitation flow).
 */
public class OrganizationMembership {

	private final OrganizationMembershipId id;
	private final OrganizationId organizationId;
	private final AccountId accountId;
	private final UUID athleteId;
	private final OrganizationMembershipRole role;
	private OrganizationMembershipStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private OrganizationMembership(
			OrganizationMembershipId id,
			OrganizationId organizationId,
			AccountId accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			OrganizationMembershipStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "OrganizationMembership id must not be null");
		this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.athleteId = athleteId;
		this.role = Objects.requireNonNull(role, "role must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		if (athleteId != null && role != OrganizationMembershipRole.ATHLETE) {
			throw new IllegalArgumentException("athleteId is only allowed for ATHLETE role");
		}
	}

	/**
	 * Bootstrap factory for organization creation: ACTIVE ORG_OWNER with null athleteId.
	 */
	public static OrganizationMembership registerOwner(
			OrganizationMembershipId id,
			OrganizationId organizationId,
			AccountId accountId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new OrganizationMembership(
				id,
				organizationId,
				accountId,
				null,
				OrganizationMembershipRole.ORG_OWNER,
				OrganizationMembershipStatus.ACTIVE,
				now,
				now,
				0L);
	}

	public static OrganizationMembership rehydrate(
			OrganizationMembershipId id,
			OrganizationId organizationId,
			AccountId accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			OrganizationMembershipStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new OrganizationMembership(
				id,
				organizationId,
				accountId,
				athleteId,
				role,
				status,
				createdAt,
				updatedAt,
				version);
	}

	public OrganizationMembershipId id() {
		return id;
	}

	public OrganizationId organizationId() {
		return organizationId;
	}

	public AccountId accountId() {
		return accountId;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public OrganizationMembershipRole role() {
		return role;
	}

	public OrganizationMembershipStatus status() {
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
