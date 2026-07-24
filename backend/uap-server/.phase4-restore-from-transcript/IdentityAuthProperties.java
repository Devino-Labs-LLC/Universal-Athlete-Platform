package com.devinolabs.uap.identity.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.identity.auth")
public class IdentityAuthProperties {

	static final int MIN_ACCESS_TOKEN_SECRET_LENGTH = 32;

	private String accessTokenSecret;
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(30);
	private int maxFailedAttempts = 5;
	private Duration lockoutDuration = Duration.ofMinutes(15);

	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = refreshTokenTtl;
	}

	public int getMaxFailedAttempts() {
		return maxFailedAttempts;
	}

	public void setMaxFailedAttempts(int maxFailedAttempts) {
		this.maxFailedAttempts = maxFailedAttempts;
	}

	public Duration getLockoutDuration() {
		return lockoutDuration;
	}

	public void setLockoutDuration(Duration lockoutDuration) {
		this.lockoutDuration = lockoutDuration;
	}

	public void validate() {
		validateAccessTokenSecret();
		requirePositiveDuration(accessTokenTtl, "uap.identity.auth.access-token-ttl");
		requirePositiveDuration(refreshTokenTtl, "uap.identity.auth.refresh-token-ttl");
		requirePositiveDuration(lockoutDuration, "uap.identity.auth.lockout-duration");
		if (maxFailedAttempts < 1) {
			throw new IllegalStateException("uap.identity.auth.max-failed-attempts must be at least 1");
		}
	}

	public void validateAccessTokenSecret() {
		if (accessTokenSecret == null || accessTokenSecret.isBlank()) {
			throw new IllegalStateException(
					"uap.identity.auth.access-token-secret must be configured via UAP_IDENTITY_ACCESS_TOKEN_SECRET");
		}
		if (accessTokenSecret.length() < MIN_ACCESS_TOKEN_SECRET_LENGTH) {
			throw new IllegalStateException(
					"uap.identity.auth.access-token-secret must be at least "
							+ MIN_ACCESS_TOKEN_SECRET_LENGTH + " characters");
		}
	}

	private static void requirePositiveDuration(Duration duration, String propertyName) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			throw new IllegalStateException(propertyName + " must be a positive duration");
		}
	}

}
