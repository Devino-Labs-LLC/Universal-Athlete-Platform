package com.devinolabs.uap.identity.application;

import java.util.Objects;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.domain.RefreshSessionId;

public final class AuthenticationResult {

	private final AccountId accountId;
	private final IssuedAccessToken accessToken;
	private final String refreshToken;
	private final RefreshSessionId refreshSessionId;

	public AuthenticationResult(
			AccountId accountId,
			IssuedAccessToken accessToken,
			String refreshToken,
			RefreshSessionId refreshSessionId) {
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalArgumentException("refreshToken must not be blank");
		}
		this.refreshToken = refreshToken;
		this.refreshSessionId = Objects.requireNonNull(refreshSessionId, "refreshSessionId must not be null");
	}

	public AccountId accountId() {
		return accountId;
	}

	public IssuedAccessToken accessToken() {
		return accessToken;
	}

	public String refreshToken() {
		return refreshToken;
	}

	public RefreshSessionId refreshSessionId() {
		return refreshSessionId;
	}

	@Override
	public String toString() {
		return "AuthenticationResult[accountId=" + accountId + ", accessToken=****, refreshToken=****]";
	}

}
