package com.devinolabs.uap.organization.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Invitation to join an organization (ORG_ADMIN) or team (team roles). Raw tokens are never stored.
 */
public class Invitation {

	public static final Duration DEFAULT_TTL = Duration.ofDays(7);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int RAW_TOKEN_BYTES = 32;

	private final InvitationId id;
	private final OrganizationId organizationId;
	private final TeamId teamId;
	private final String invitedEmail;
	private final AccountId invitedAccountId;
	private final OrganizationMembershipRole role;
	private final String tokenHash;
	private InvitationStatus status;
	private final Instant expiresAt;
	private UUID acceptedMembershipId;
	private final AccountId createdByAccountId;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Invitation(
			InvitationId id,
			OrganizationId organizationId,
			TeamId teamId,
			String invitedEmail,
			AccountId invitedAccountId,
			OrganizationMembershipRole role,
			String tokenHash,
			InvitationStatus status,
			Instant expiresAt,
			UUID acceptedMembershipId,
			AccountId createdByAccountId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Invitation id must not be null");
		this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		this.teamId = teamId;
		this.invitedEmail = requireNormalizedEmail(invitedEmail);
		this.invitedAccountId = invitedAccountId;
		this.role = Objects.requireNonNull(role, "role must not be null");
		this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
		if (tokenHash.isBlank()) {
			throw new IllegalArgumentException("tokenHash must not be blank");
		}
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.acceptedMembershipId = acceptedMembershipId;
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId, "createdByAccountId must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		validateScopeRole();
	}

	public static IssuedInvitation issue(
			InvitationId id,
			OrganizationId organizationId,
			TeamId teamId,
			String invitedEmail,
			AccountId invitedAccountId,
			OrganizationMembershipRole role,
			AccountId createdByAccountId,
			Function<String, String> tokenDigester,
			Clock clock) {
		Objects.requireNonNull(tokenDigester, "tokenDigester must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		String rawToken = generateRawToken();
		String tokenHash = tokenDigester.apply(rawToken);
		Instant now = Instant.now(clock);
		Invitation invitation = new Invitation(
				id,
				organizationId,
				teamId,
				invitedEmail,
				invitedAccountId,
				role,
				tokenHash,
				InvitationStatus.PENDING,
				now.plus(DEFAULT_TTL),
				null,
				createdByAccountId,
				now,
				now,
				0L);
		return new IssuedInvitation(invitation, rawToken);
	}

	public static Invitation rehydrate(
			InvitationId id,
			OrganizationId organizationId,
			TeamId teamId,
			String invitedEmail,
			AccountId invitedAccountId,
			OrganizationMembershipRole role,
			String tokenHash,
			InvitationStatus status,
			Instant expiresAt,
			UUID acceptedMembershipId,
			AccountId createdByAccountId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Invitation(
				id,
				organizationId,
				teamId,
				invitedEmail,
				invitedAccountId,
				role,
				tokenHash,
				status,
				expiresAt,
				acceptedMembershipId,
				createdByAccountId,
				createdAt,
				updatedAt,
				version);
	}

	public boolean isEffective(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		return status == InvitationStatus.PENDING && !isExpired(clock);
	}

	public boolean isExpired(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		return !Instant.now(clock).isBefore(expiresAt);
	}

	public void markExpired(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status != InvitationStatus.PENDING) {
			throw new IllegalStateException("Only PENDING invitations can expire");
		}
		this.status = InvitationStatus.EXPIRED;
		this.updatedAt = Instant.now(clock);
	}

	public void acceptPending(UUID membershipId, Clock clock) {
		Objects.requireNonNull(membershipId, "membershipId must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status != InvitationStatus.PENDING) {
			throw new IllegalStateException("Only PENDING invitations can be accepted");
		}
		if (isExpired(clock)) {
			throw new IllegalStateException("Expired invitations cannot be accepted");
		}
		this.status = InvitationStatus.ACCEPTED;
		this.acceptedMembershipId = membershipId;
		this.updatedAt = Instant.now(clock);
	}

	public void decline(Clock clock) {
		requirePendingMutable(clock);
		this.status = InvitationStatus.DECLINED;
		this.updatedAt = Instant.now(clock);
	}

	public void revoke(Clock clock) {
		requirePendingMutable(clock);
		this.status = InvitationStatus.REVOKED;
		this.updatedAt = Instant.now(clock);
	}

	private void requirePendingMutable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status != InvitationStatus.PENDING) {
			throw new IllegalStateException("Only PENDING invitations can change status");
		}
		if (isExpired(clock)) {
			throw new IllegalStateException("Expired invitations cannot change status");
		}
	}

	private void validateScopeRole() {
		if (teamId == null) {
			if (role != OrganizationMembershipRole.ORG_ADMIN) {
				throw new IllegalArgumentException("Org-scoped invitations must use ORG_ADMIN");
			}
			return;
		}
		if (role != OrganizationMembershipRole.ATHLETE
				&& role != OrganizationMembershipRole.COACH
				&& role != OrganizationMembershipRole.HEAD_COACH
				&& role != OrganizationMembershipRole.TEAM_ADMIN) {
			throw new IllegalArgumentException("Team-scoped invitations must use a team role");
		}
	}

	private static String requireNormalizedEmail(String email) {
		Objects.requireNonNull(email, "invitedEmail must not be null");
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("invitedEmail must not be blank");
		}
		if (normalized.length() > 320) {
			throw new IllegalArgumentException("invitedEmail must not exceed 320 characters");
		}
		return normalized;
	}

	private static String generateRawToken() {
		byte[] bytes = new byte[RAW_TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public InvitationId id() {
		return id;
	}

	public OrganizationId organizationId() {
		return organizationId;
	}

	public TeamId teamId() {
		return teamId;
	}

	public boolean isOrgScoped() {
		return teamId == null;
	}

	public String invitedEmail() {
		return invitedEmail;
	}

	public AccountId invitedAccountId() {
		return invitedAccountId;
	}

	public OrganizationMembershipRole role() {
		return role;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public InvitationStatus status() {
		return status;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public UUID acceptedMembershipId() {
		return acceptedMembershipId;
	}

	public AccountId createdByAccountId() {
		return createdByAccountId;
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
