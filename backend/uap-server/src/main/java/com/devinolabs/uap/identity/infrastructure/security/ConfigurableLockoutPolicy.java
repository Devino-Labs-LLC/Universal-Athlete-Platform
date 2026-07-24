package com.devinolabs.uap.identity.infrastructure.security;

import java.time.Duration;
import java.util.Objects;

import com.devinolabs.uap.identity.domain.LockoutPolicy;
import com.devinolabs.uap.identity.infrastructure.IdentityAuthProperties;

public final class ConfigurableLockoutPolicy implements LockoutPolicy {

	private final int maxFailedAttempts;
	private final Duration lockDuration;

	public ConfigurableLockoutPolicy(IdentityAuthProperties properties) {
		Objects.requireNonNull(properties, "properties must not be null");
		if (properties.getMaxFailedAttempts() < 1) {
			throw new IllegalArgumentException("maxFailedAttempts must be at least 1");
		}
		this.maxFailedAttempts = properties.getMaxFailedAttempts();
		this.lockDuration = Objects.requireNonNull(properties.getLockoutDuration(), "lockoutDuration must not be null");
		if (lockDuration.isNegative() || lockDuration.isZero()) {
			throw new IllegalArgumentException("lockoutDuration must be positive");
		}
	}

	@Override
	public int maxFailedAttempts() {
		return maxFailedAttempts;
	}

	@Override
	public Duration lockDuration() {
		return lockDuration;
	}

}
