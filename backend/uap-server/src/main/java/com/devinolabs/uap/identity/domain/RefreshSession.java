package com.devinolabs.uap.identity.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class RefreshSession {

	private static final int RAW_TOKEN_BYTES = 32;

	private final RefreshSessionId id;
	private final AccountId accountId;
	private final String tokenDigest;
	private final Instant createdAt;
	private final Instant expiresAt;
	private Instant lastUsedAt;
	private Instant revokedAt;
	private RefreshSessionId replacedBySessionId;
	private RefreshSessionRevocationReason revocationReason;
	private long version;

	private RefreshSession(
			RefreshSessionId id,
			AccountId accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant lastUsedAt,
			Instant revokedAt,
			RefreshSessionId replacedBySessionId,
			RefreshSessionRevocationReason revocationReason,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		if (tokenDigest == null || tokenDigest.isBlank()) {
			throw new IllegalArgumentException("tokenDigest must not be blank");
		}
		this.tokenDigest = tokenDigest;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (expiresAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("expiresAt must not be before createdAt");
		}
		this.lastUsedAt = lastUsedAt;
		this.revokedAt = revokedAt;
		this.replacedBySessionId = replacedBySessionId;
		this.revocationReason = revocationReason;
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		this.version = version;
	}

	public static IssuedRefreshSession issue(
			AccountId accountId,
			TokenDigester tokenDigester,
			Duration timeToLive,
			Clock clock) {
		return issue(accountId, tokenDigester, timeToLive, clock, new SecureRandom());
	}

	public static IssuedRefreshSession issue(
			AccountId accountId,
			TokenDigester tokenDigester,
			Duration timeToLive,
			Clock clock,
			SecureRandom secureRandom) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(tokenDigester, "tokenDigester must not be null");
		Objects.requireNonNull(timeToLive, "timeToLive must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		Objects.requireNonNull(secureRandom, "secureRandom must not be null");
		if (timeToLive.isNegative() || timeToLive.isZero()) {
			throw new IllegalArgumentException("timeToLive must be positive");
		}

		byte[] rawBytes = new byte[RAW_TOKEN_BYTES];
		secureRandom.nextBytes(rawBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
		Instant createdAt = Instant.now(clock);

		RefreshSession session = new RefreshSession(
				RefreshSessionId.generate(),
				accountId,
				tokenDigester.digest(rawToken),
				createdAt,
				createdAt.plus(timeToLive),
				createdAt,
				null,
				null,
				null,
				0L);

		return new IssuedRefreshSession(session, rawToken);
	}

	public static RefreshSession rehydrate(
			RefreshSessionId id,
			AccountId accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant lastUsedAt,
			Instant revokedAt,
			RefreshSessionId replacedBySessionId,
			RefreshSessionRevocationReason revocationReason,
			long version) {
		return new RefreshSession(
				id,
				accountId,
				tokenDigest,
				createdAt,
				expiresAt,
				lastUsedAt,
				revokedAt,
				replacedBySessionId,
				revocationReason,
				version);
	}

	public boolean isExpired(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		return !Instant.now(clock).isBefore(expiresAt);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isActive(Clock clock) {
		return !isRevoked() && !isExpired(clock);
	}

	public void markUsed(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		this.lastUsedAt = Instant.now(clock);
	}

	public void revoke(RefreshSessionRevocationReason reason, Clock clock) {
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		if (isRevoked()) {
			return;
		}
		this.revokedAt = Instant.now(clock);
		this.revocationReason = reason;
	}

	public void rotateTo(RefreshSessionId replacementSessionId, Clock clock) {
		Objects.requireNonNull(replacementSessionId, "replacementSessionId must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		if (isRevoked()) {
			throw new IllegalStateException("Revoked refresh session cannot be rotated");
		}
		if (isExpired(clock)) {
			throw new IllegalStateException("Expired refresh session cannot be rotated");
		}
		if (replacedBySessionId != null) {
			throw new IllegalStateException("Refresh session has already been replaced");
		}
		revoke(RefreshSessionRevocationReason.ROTATED, clock);
		this.replacedBySessionId = replacementSessionId;
	}

	public RefreshSessionId id() {
		return id;
	}

	public AccountId accountId() {
		return accountId;
	}

	public String tokenDigest() {
		return tokenDigest;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public Instant lastUsedAt() {
		return lastUsedAt;
	}

	public Instant revokedAt() {
		return revokedAt;
	}

	public RefreshSessionId replacedBySessionId() {
		return replacedBySessionId;
	}

	public RefreshSessionRevocationReason revocationReason() {
		return revocationReason;
	}

	public long version() {
		return version;
	}

	@Override
	public String toString() {
		return "RefreshSession[id=" + id + ", accountId=" + accountId + ", digest=****]";
	}

}
