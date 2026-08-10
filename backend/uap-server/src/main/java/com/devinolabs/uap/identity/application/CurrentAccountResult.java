package com.devinolabs.uap.identity.application;

import java.time.Instant;
import java.util.Objects;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;

public final class CurrentAccountResult {

	private final AccountId accountId;
	private final EmailAddress email;
	private final AccountStatus status;
	private final Instant emailVerifiedAt;

	public CurrentAccountResult(
			AccountId accountId,
			EmailAddress email,
			AccountStatus status,
			Instant emailVerifiedAt) {
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public AccountId accountId() {
		return accountId;
	}

	public EmailAddress email() {
		return email;
	}

	public AccountStatus status() {
		return status;
	}

	public Instant emailVerifiedAt() {
		return emailVerifiedAt;
	}

}
