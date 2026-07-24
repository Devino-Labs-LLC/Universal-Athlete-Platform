package com.devinolabs.uap.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.identity.domain.AccountStatus;

@Entity
@Table(name = "accounts")
class AccountJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "email", nullable = false, length = 320, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AccountStatus status;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AccountJpaEntity() {
	}

	AccountJpaEntity(
			UUID id,
			String email,
			String passwordHash,
			AccountStatus status,
			int failedLoginAttempts,
			Instant lockedUntil,
			Instant emailVerifiedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.status = status;
		this.failedLoginAttempts = failedLoginAttempts;
		this.lockedUntil = lockedUntil;
		this.emailVerifiedAt = emailVerifiedAt;
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

	String getEmail() {
		return email;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	AccountStatus getStatus() {
		return status;
	}

	int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	Instant getLockedUntil() {
		return lockedUntil;
	}

	Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
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
