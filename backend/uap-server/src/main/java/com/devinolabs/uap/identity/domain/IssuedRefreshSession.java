package com.devinolabs.uap.identity.domain;

import java.util.Objects;

public final class IssuedRefreshSession {

	private final RefreshSession session;
	private final String rawToken;

	public IssuedRefreshSession(RefreshSession session, String rawToken) {
		this.session = Objects.requireNonNull(session, "session must not be null");
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken must not be blank");
		}
		this.rawToken = rawToken;
	}

	public RefreshSession session() {
		return session;
	}

	public String rawToken() {
		return rawToken;
	}

	@Override
	public String toString() {
		return "IssuedRefreshSession[sessionId=" + session.id() + ", rawToken=****]";
	}

}
