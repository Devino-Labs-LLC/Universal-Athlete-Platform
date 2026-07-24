package com.devinolabs.uap.identity.domain;

import java.time.Instant;
import java.util.Objects;

public final class IssuedAccessToken {

	private final String token;
	private final AccessTokenClaims claims;

	public IssuedAccessToken(String token, AccessTokenClaims claims) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token must not be blank");
		}
		this.token = token;
		this.claims = Objects.requireNonNull(claims, "claims must not be null");
	}

	public String token() {
		return token;
	}

	public AccessTokenClaims claims() {
		return claims;
	}

	public String tokenId() {
		return claims.tokenId();
	}

	public Instant issuedAt() {
		return claims.issuedAt();
	}

	public Instant expiresAt() {
		return claims.expiresAt();
	}

	@Override
	public String toString() {
		return "IssuedAccessToken[tokenId=" + claims.tokenId() + ", token=****]";
	}

}
