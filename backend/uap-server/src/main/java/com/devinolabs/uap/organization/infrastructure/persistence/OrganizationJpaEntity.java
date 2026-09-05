package com.devinolabs.uap.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.devinolabs.uap.organization.domain.OrganizationStatus;

@Entity
@Table(name = "organizations")
class OrganizationJpaEntity extends AbstractPersistableUuidJpaEntity {

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrganizationStatus status;

	protected OrganizationJpaEntity() {
	}

	OrganizationJpaEntity(
			UUID id,
			String name,
			OrganizationStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.name = name;
		this.status = status;
	}

	String getName() {
		return name;
	}

	OrganizationStatus getStatus() {
		return status;
	}

	void applyDomainState(String name, OrganizationStatus status, Instant updatedAt) {
		this.name = name;
		this.status = status;
		this.updatedAt = updatedAt;
	}

}
