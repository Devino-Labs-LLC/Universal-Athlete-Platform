package com.devinolabs.uap.aicoach.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-account fixed window, matching the identity HTTP limit shape (capacity per window).
 */
public final class CoachRateLimiter {

	private final int capacity;
	private final Duration window;
	private final Clock clock;
	private final ConcurrentHashMap<UUID, Deque<Instant>> hits = new ConcurrentHashMap<>();

	public CoachRateLimiter(int capacity, Duration window, Clock clock) {
		this.capacity = capacity;
		this.window = window;
		this.clock = clock;
	}

	public void acquire(UUID accountId) {
		acquire(accountId, clock.instant());
	}

	void acquire(UUID accountId, Instant now) {
		Deque<Instant> deque = hits.computeIfAbsent(accountId, ignored -> new ArrayDeque<>());
		synchronized (deque) {
			Instant cutoff = now.minus(window);
			while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
				deque.removeFirst();
			}
			if (deque.size() >= capacity) {
				throw new CoachRateLimitedException();
			}
			deque.addLast(now);
		}
	}

}
