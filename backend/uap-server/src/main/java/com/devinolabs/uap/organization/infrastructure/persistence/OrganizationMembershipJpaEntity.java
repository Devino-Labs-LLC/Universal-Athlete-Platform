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

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

@Entity
@Table(name = "organization_memberships")
class OrganizationMembershipJpaEntity extends AbstractPersistableUuidJpaEntity {

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 30)
	private OrganizationMembershipRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrganizationMembershipStatus status;

	protected OrganizationMembershipJpaEntity() {
	}

	OrganizationMembershipJpaEntity(
			UUID id,
			UUID organizationId,
			UUID accountId,
			UUID athleteId,
			OrganizationMembershipRole role,
			OrganizationMembershipStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.organizationId = organizationId;
		this.accountId = accountId;
		this.athleteId = athleteId;
		this.role = role;
		this.status = status;
	}

	UUID getOrganizationId() {
		return organizationId;
	}

	UUID getAccountId() {
		return accountId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	OrganizationMembershipRole getRole() {
		return role;
	}

	OrganizationMembershipStatus getStatus() {
		return status;
	}

}
