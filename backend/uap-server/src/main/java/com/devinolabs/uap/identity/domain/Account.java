package com.devinolabs.uap.identity.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class Account {

	private final AccountId id;
	private final EmailAddress email;
	private PasswordCredential passwordCredential;
	private AccountStatus status;
	private int failedLoginAttempts;
	private Instant lockedUntil;
	private Instant emailVerifiedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Account(
			AccountId id,
			EmailAddress email,
			PasswordCredential passwordCredential,
			AccountStatus status,
			int failedLoginAttempts,
			Instant lockedUntil,
			Instant emailVerifiedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Account id must not be null");
		this.email = Objects.requireNonNull(email, "Account email must not be null");
		this.passwordCredential = Objects.requireNonNull(passwordCredential, "Account password credential must not be null");
		this.status = Objects.requireNonNull(status, "Account status must not be null");
		if (failedLoginAttempts < 0) {
			throw new IllegalArgumentException("Failed login attempts must not be negative");
		}
		this.failedLoginAttempts = failedLoginAttempts;
		this.lockedUntil = lockedUntil;
		this.emailVerifiedAt = emailVerifiedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "Account createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "Account updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		enforceLockStateInvariants();
	}

	private void enforceLockStateInvariants() {
		if (status == AccountStatus.LOCKED) {
			if (lockedUntil == null) {
				throw new IllegalArgumentException("LOCKED account requires lockedUntil");
			}
			return;
		}
		if (lockedUntil != null) {
			throw new IllegalArgumentException(status + " account cannot retain lockedUntil");
		}
	}

	public static Account register(AccountId id, EmailAddress email, PasswordCredential passwordCredential) {
		return register(id, email, passwordCredential, Clock.systemUTC());
	}

	public static Account register(AccountId id, EmailAddress email, PasswordCredential passwordCredential, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Account(
				id,
				email,
				passwordCredential,
				AccountStatus.PENDING_VERIFICATION,
				0,
				null,
				null,
				now,
				now,
				0L);
	}

	public static Account rehydrate(
			AccountId id,
			EmailAddress email,
			PasswordCredential passwordCredential,
			AccountStatus status,
			int failedLoginAttempts,
			Instant lockedUntil,
			Instant emailVerifiedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Account(
				id,
				email,
				passwordCredential,
				status,
				failedLoginAttempts,
				lockedUntil,
				emailVerifiedAt,
				createdAt,
				updatedAt,
				version);
	}

	public void verifyEmail(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AccountStatus.DISABLED) {
			throw new IllegalStateException("Disabled account cannot be verified");
		}
		if (status == AccountStatus.LOCKED) {
			throw new IllegalStateException("Locked account cannot be verified");
		}
		Instant now = Instant.now(clock);
		this.status = AccountStatus.ACTIVE;
		this.emailVerifiedAt = now;
		this.updatedAt = now;
	}

	public boolean isCurrentlyLocked(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		return status == AccountStatus.LOCKED
				&& lockedUntil != null
				&& Instant.now(clock).isBefore(lockedUntil);
	}

	public void clearExpiredLock(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status != AccountStatus.LOCKED || lockedUntil == null) {
			return;
		}
		if (Instant.now(clock).isBefore(lockedUntil)) {
			return;
		}
		Instant now = Instant.now(clock);
		this.status = AccountStatus.ACTIVE;
		this.lockedUntil = null;
		this.failedLoginAttempts = 0;
		this.updatedAt = now;
	}

	public void recordFailedAuthentication(LockoutPolicy lockoutPolicy, Clock clock) {
		Objects.requireNonNull(lockoutPolicy, "LockoutPolicy must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AccountStatus.DISABLED || status == AccountStatus.PENDING_VERIFICATION) {
			return;
		}
		clearExpiredLock(clock);
		if (isCurrentlyLocked(clock)) {
			return;
		}

		Instant now = Instant.now(clock);
		this.failedLoginAttempts += 1;
		if (this.failedLoginAttempts >= lockoutPolicy.maxFailedAttempts()) {
			this.status = AccountStatus.LOCKED;
			this.lockedUntil = now.plus(lockoutPolicy.lockDuration());
		}
		this.updatedAt = now;
	}

	public void recordSuccessfulAuthentication(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		clearExpiredLock(clock);
		Instant now = Instant.now(clock);
		this.failedLoginAttempts = 0;
		this.lockedUntil = null;
		if (this.status == AccountStatus.LOCKED) {
			this.status = AccountStatus.ACTIVE;
		}
		this.updatedAt = now;
	}

	public AccountId id() {
		return id;
	}

	public EmailAddress email() {
		return email;
	}

	public PasswordCredential passwordCredential() {
		return passwordCredential;
	}

	public AccountStatus status() {
		return status;
	}

	public int failedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant lockedUntil() {
		return lockedUntil;
	}

	public Instant emailVerifiedAt() {
		return emailVerifiedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
