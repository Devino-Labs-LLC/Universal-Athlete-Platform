package com.devinolabs.uap.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationTokenJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@Column(name = "token_digest", nullable = false, length = 64, unique = true)
	private String tokenDigest;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected EmailVerificationTokenJpaEntity() {
	}

	EmailVerificationTokenJpaEntity(
			UUID id,
			UUID accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant consumedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.accountId = accountId;
		this.tokenDigest = tokenDigest;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.consumedAt = consumedAt;
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

	UUID getAccountId() {
		return accountId;
	}

	String getTokenDigest() {
		return tokenDigest;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	Instant getConsumedAt() {
		return consumedAt;
	}

	long getVersion() {
		return version;
	}

}
