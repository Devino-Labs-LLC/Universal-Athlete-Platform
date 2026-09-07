package com.devinolabs.uap.consent.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.consent.domain.ConsentGrantStatus;
import com.devinolabs.uap.consent.domain.ConsentScope;

@Entity
@Table(name = "consent_grants")
class ConsentGrantJpaEntity extends AbstractPersistableUuidJpaEntity {

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID teamId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_membership_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID teamMembershipId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ConsentGrantStatus status;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "consent_grant_scopes",
			joinColumns = @JoinColumn(name = "consent_grant_id", columnDefinition = "BINARY(16)"))
	@Column(name = "scope", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<ConsentScope> scopes = new LinkedHashSet<>();

	protected ConsentGrantJpaEntity() {
	}

	ConsentGrantJpaEntity(
			UUID id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			ConsentGrantStatus status,
			Instant createdAt,
			Instant revokedAt,
			Instant updatedAt,
			long version,
			Set<ConsentScope> scopes,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.athleteId = athleteId;
		this.teamId = teamId;
		this.organizationId = organizationId;
		this.teamMembershipId = teamMembershipId;
		this.status = status;
		this.revokedAt = revokedAt;
		this.scopes = scopes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(scopes);
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getTeamId() {
		return teamId;
	}

	UUID getOrganizationId() {
		return organizationId;
	}

	UUID getTeamMembershipId() {
		return teamMembershipId;
	}

	ConsentGrantStatus getStatus() {
		return status;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}

	Set<ConsentScope> getScopes() {
		return scopes;
	}

	void applyDomainState(ConsentGrantStatus status, Instant revokedAt, Instant updatedAt) {
		this.status = status;
		this.revokedAt = revokedAt;
		this.updatedAt = updatedAt;
	}

}
