package com.devinolabs.uap.organization.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Team-scoped membership. Application assigns team-appropriate roles only.
 */
public class TeamMembership {

	private static final Set<OrganizationMembershipRole> TEAM_ROLES = EnumSet.of(
			OrganizationMembershipRole.ATHLETE,
			OrganizationMembershipRole.COACH,
			OrganizationMembershipRole.HEAD_COACH,
			OrganizationMembershipRole.TEAM_ADMIN);

	private final TeamMembershipId id;
	private final TeamId teamId;
	private final AccountId accountId;
	private final UUID athleteId;
	private final OrganizationMembershipRole role;
	private OrganizationMembershipStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private TeamMembership(
			TeamMembershipId id,
			TeamId teamId,
			AccountId accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			OrganizationMembershipStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "TeamMembership id must not be null");
		this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
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
		if (!TEAM_ROLES.contains(role)) {
			throw new IllegalArgumentException("Team membership role must be a team role");
		}
		if (athleteId != null && role != OrganizationMembershipRole.ATHLETE) {
			throw new IllegalArgumentException("athleteId is only allowed for ATHLETE role");
		}
		if (role == OrganizationMembershipRole.ATHLETE && athleteId == null) {
			throw new IllegalArgumentException("ATHLETE team membership requires athleteId");
		}
	}

	public static TeamMembership register(
			TeamMembershipId id,
			TeamId teamId,
			AccountId accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new TeamMembership(
				id,
				teamId,
				accountId,
				athleteId,
				role,
				OrganizationMembershipStatus.ACTIVE,
				now,
				now,
				0L);
	}

	public static TeamMembership rehydrate(
			TeamMembershipId id,
			TeamId teamId,
			AccountId accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			OrganizationMembershipStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new TeamMembership(
				id,
				teamId,
				accountId,
				athleteId,
				role,
				status,
				createdAt,
				updatedAt,
				version);
	}

	public void remove(Clock clock) {
		transitionTo(OrganizationMembershipStatus.REMOVED, clock);
	}

	public void leave(Clock clock) {
		transitionTo(OrganizationMembershipStatus.LEFT, clock);
	}

	private void transitionTo(OrganizationMembershipStatus target, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status != OrganizationMembershipStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE memberships can transition to " + target);
		}
		this.status = target;
		this.updatedAt = Instant.now(clock);
	}

	public boolean isActive() {
		return status == OrganizationMembershipStatus.ACTIVE;
	}

	public TeamMembershipId id() {
		return id;
	}

	public TeamId teamId() {
		return teamId;
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
