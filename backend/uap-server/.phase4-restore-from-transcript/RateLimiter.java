package com.devinolabs.uap.identity.infrastructure.http;

/**
 * Replaceable rate-limiting boundary. Local/tests use {@link InMemoryRateLimiter};
 * production can swap in a Redis-backed implementation without changing filters.
 */
public interface RateLimiter {

	boolean tryAcquire(String key);

}
