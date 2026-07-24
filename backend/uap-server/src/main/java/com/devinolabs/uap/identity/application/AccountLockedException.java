package com.devinolabs.uap.identity.application;

import java.time.Instant;

public class AccountLockedException extends RuntimeException {

	private final Instant lockedUntil;

	public AccountLockedException(Instant lockedUntil) {
		super("Account is locked until " + lockedUntil);
		this.lockedUntil = lockedUntil;
	}

	public Instant lockedUntil() {
		return lockedUntil;
	}

}
