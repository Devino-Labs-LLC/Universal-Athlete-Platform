package com.devinolabs.uap.identity.infrastructure.http;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter implements RateLimiter {

	private final IdentityHttpProperties.RateLimit properties;
	private final Clock clock;
	private final Map<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

	public InMemoryRateLimiter(IdentityHttpProperties.RateLimit properties, Clock clock) {
		this.properties = Objects.requireNonNull(properties);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override

	public boolean tryAcquire(String key) {
		Objects.requireNonNull(key, "key must not be null");
		Instant now = Instant.now(clock);
		Instant windowStart = now.minus(properties.getWindow());
		Deque<Instant> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		synchronized (timestamps) {
			while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
				timestamps.removeFirst();
			}
			if (timestamps.size() >= properties.getCapacity()) {
				return false;
			}
			timestamps.addLast(now);
			return true;
		}
	}

}
