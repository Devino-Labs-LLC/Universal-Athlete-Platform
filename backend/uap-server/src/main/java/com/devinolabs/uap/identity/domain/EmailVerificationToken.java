package com.devinolabs.uap.identity.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class EmailVerificationToken {

	public static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofHours(24);

	private static final int RAW_TOKEN_BYTES = 32;

	private final EmailVerificationTokenId id;
	private final AccountId accountId;
	private final String tokenDigest;
	private final Instant createdAt;
	private final Instant expiresAt;
	private Instant consumedAt;
	private long version;

	private EmailVerificationToken(
			EmailVerificationTokenId id,
			AccountId accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant consumedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Token id must not be null");
		this.accountId = Objects.requireNonNull(accountId, "Account id must not be null");
		if (tokenDigest == null || tokenDigest.isBlank()) {
			throw new IllegalArgumentException("Token digest must not be blank");
		}
		this.tokenDigest = tokenDigest;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (expiresAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("expiresAt must not be before createdAt");
		}
		this.consumedAt = consumedAt;
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static IssuedEmailVerificationToken issue(
			AccountId accountId,
			TokenDigester tokenDigester,
			Clock clock) {
		return issue(accountId, tokenDigester, clock, DEFAULT_TIME_TO_LIVE, new SecureRandom());
	}

	public static IssuedEmailVerificationToken issue(
			AccountId accountId,
			TokenDigester tokenDigester,
			Clock clock,
			Duration timeToLive,
			SecureRandom secureRandom) {
		Objects.requireNonNull(accountId, "Account id must not be null");
		Objects.requireNonNull(tokenDigester, "TokenDigester must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(timeToLive, "timeToLive must not be null");
		Objects.requireNonNull(secureRandom, "SecureRandom must not be null");
		if (timeToLive.isNegative() || timeToLive.isZero()) {
			throw new IllegalArgumentException("timeToLive must be positive");
		}

		byte[] rawBytes = new byte[RAW_TOKEN_BYTES];
		secureRandom.nextBytes(rawBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
		Instant createdAt = Instant.now(clock);

		EmailVerificationToken token = new EmailVerificationToken(
				EmailVerificationTokenId.generate(),
				accountId,
				tokenDigester.digest(rawToken),
				createdAt,
				createdAt.plus(timeToLive),
				null,
				0L);

		return new IssuedEmailVerificationToken(token, rawToken);
	}

	public static EmailVerificationToken rehydrate(
			EmailVerificationTokenId id,
			AccountId accountId,
			String tokenDigest,
			Instant createdAt,
			Instant expiresAt,
			Instant consumedAt,
			long version) {
		return new EmailVerificationToken(id, accountId, tokenDigest, createdAt, expiresAt, consumedAt, version);
	}

	public boolean isExpired(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		return !Instant.now(clock).isBefore(expiresAt);
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

	public void ensureUsable(Clock clock) {
		if (isConsumed()) {
			throw new TokenAlreadyConsumedException();
		}
		if (isExpired(clock)) {
			throw new TokenExpiredException();
		}
	}

	public void consume(Clock clock) {
		ensureUsable(clock);
		this.consumedAt = Instant.now(clock);
	}

	public EmailVerificationTokenId id() {
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

	public Instant consumedAt() {
		return consumedAt;
	}

	public long version() {
		return version;
	}

	@Override
	public String toString() {
		return "EmailVerificationToken[id=" + id + ", accountId=" + accountId + ", digest=****]";
	}

}
