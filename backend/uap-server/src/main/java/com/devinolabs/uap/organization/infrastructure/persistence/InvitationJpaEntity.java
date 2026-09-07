package com.devinolabs.uap.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

@Entity
@Table(name = "invitations")
class InvitationJpaEntity extends AbstractPersistableUuidJpaEntity {

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID teamId;

	@Column(name = "invited_email", nullable = false, updatable = false, length = 320)
	private String invitedEmail;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "invited_account_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID invitedAccountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, updatable = false, length = 30)
	private OrganizationMembershipRole role;

	@Column(name = "token_hash", nullable = false, updatable = false, length = 128)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InvitationStatus status;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "accepted_membership_id", columnDefinition = "BINARY(16)")
	private UUID acceptedMembershipId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "created_by_account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID createdByAccountId;

	protected InvitationJpaEntity() {
	}

	InvitationJpaEntity(
			UUID id,
			UUID organizationId,
			UUID teamId,
			String invitedEmail,
			UUID invitedAccountId,
			OrganizationMembershipRole role,
			String tokenHash,
			InvitationStatus status,
			Instant expiresAt,
			UUID acceptedMembershipId,
			UUID createdByAccountId,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.organizationId = organizationId;
		this.teamId = teamId;
		this.invitedEmail = invitedEmail;
		this.invitedAccountId = invitedAccountId;
		this.role = role;
		this.tokenHash = tokenHash;
		this.status = status;
		this.expiresAt = expiresAt;
		this.acceptedMembershipId = acceptedMembershipId;
		this.createdByAccountId = createdByAccountId;
	}

	UUID getOrganizationId() {
		return organizationId;
	}

	UUID getTeamId() {
		return teamId;
	}

	String getInvitedEmail() {
		return invitedEmail;
	}

	UUID getInvitedAccountId() {
		return invitedAccountId;
	}

	OrganizationMembershipRole getRole() {
		return role;
	}

	String getTokenHash() {
		return tokenHash;
	}

	InvitationStatus getStatus() {
		return status;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	UUID getAcceptedMembershipId() {
		return acceptedMembershipId;
	}

	UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	void applyDomainState(InvitationStatus status, UUID acceptedMembershipId, Instant updatedAt) {
		this.status = status;
		this.acceptedMembershipId = acceptedMembershipId;
		this.updatedAt = updatedAt;
	}

}
