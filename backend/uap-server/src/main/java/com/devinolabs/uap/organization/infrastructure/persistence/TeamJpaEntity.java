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

import com.devinolabs.uap.organization.domain.TeamStatus;

@Entity
@Table(name = "teams")
class TeamJpaEntity extends AbstractPersistableUuidJpaEntity {

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TeamStatus status;

	protected TeamJpaEntity() {
	}

	TeamJpaEntity(
			UUID id,
			UUID organizationId,
			String name,
			TeamStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.organizationId = organizationId;
		this.name = name;
		this.status = status;
	}

	UUID getOrganizationId() {
		return organizationId;
	}

	String getName() {
		return name;
	}

	TeamStatus getStatus() {
		return status;
	}

	void applyDomainState(String name, TeamStatus status, Instant updatedAt) {
		this.name = name;
		this.status = status;
		this.updatedAt = updatedAt;
	}

}
