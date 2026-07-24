package com.devinolabs.uap.identity.domain;

import java.time.Instant;
import java.util.Objects;

public final class AccessTokenClaims {

	private final String tokenId;
	private final AccountId accountId;
	private final Instant issuedAt;
	private final Instant expiresAt;

	public AccessTokenClaims(String tokenId, AccountId accountId, Instant issuedAt, Instant expiresAt) {
		if (tokenId == null || tokenId.isBlank()) {
			throw new IllegalArgumentException("tokenId must not be blank");
		}
		this.tokenId = tokenId;
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}

	public String tokenId() {
		return tokenId;
	}

	public AccountId accountId() {
		return accountId;
	}

	public Instant issuedAt() {
		return issuedAt;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

}
