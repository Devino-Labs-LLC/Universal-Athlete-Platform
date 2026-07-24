package com.devinolabs.uap.identity.domain;

import java.util.Objects;

public final class IssuedEmailVerificationToken {

	private final EmailVerificationToken token;
	private final String rawToken;

	public IssuedEmailVerificationToken(EmailVerificationToken token, String rawToken) {
		this.token = Objects.requireNonNull(token, "token must not be null");
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken must not be blank");
		}
		this.rawToken = rawToken;
	}

	public EmailVerificationToken token() {
		return token;
	}

	public String rawToken() {
		return rawToken;
	}

	@Override
	public String toString() {
		return "IssuedEmailVerificationToken[tokenId=" + token.id() + ", rawToken=****]";
	}

}
