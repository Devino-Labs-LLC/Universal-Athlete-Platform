package com.devinolabs.uap.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@MappedSuperclass
abstract class AbstractPersistableUuidJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	protected UUID id;

	@Column(name = "created_at", nullable = false, updatable = false)
	protected Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	protected Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	protected long version;

	@Transient
	protected boolean isNew = true;

	protected AbstractPersistableUuidJpaEntity() {
	}

	protected AbstractPersistableUuidJpaEntity(
			UUID id,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

}
