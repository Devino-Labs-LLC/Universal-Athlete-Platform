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

import com.devinolabs.uap.identity.domain.RefreshSessionRevocationReason;

@Entity
@Table(name = "refresh_sessions")
class RefreshSessionJpaEntity implements Persistable<UUID> {

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

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "replaced_by_session_id", columnDefinition = "BINARY(16)")
	private UUID replacedBySessionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "revocation_reason", length = 32)
	private RefreshSessionRevocationReason revocationReason;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected RefreshSessionJpaEntity() {
	}

	RefreshSessionJpaEntity(
			UUID id,
			UUID accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant lastUsedAt,
			Instant revokedAt,
			UUID replacedBySessionId,
			RefreshSessionRevocationReason revocationReason,
			long version,
			boolean isNew) {
		this.id = id;
		this.accountId = accountId;
		this.tokenDigest = tokenDigest;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.lastUsedAt = lastUsedAt;
		this.revokedAt = revokedAt;
		this.replacedBySessionId = replacedBySessionId;
		this.revocationReason = revocationReason;
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

	Instant getLastUsedAt() {
		return lastUsedAt;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}

	UUID getReplacedBySessionId() {
		return replacedBySessionId;
	}

	RefreshSessionRevocationReason getRevocationReason() {
		return revocationReason;
	}

	long getVersion() {
		return version;
	}

}
