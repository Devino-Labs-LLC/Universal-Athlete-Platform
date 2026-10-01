package com.devinolabs.uap.integrations.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * MVP one-active-provider rule (ADR-048 D7 = A).
 *
 * <p>Persistence keeps {@code UNIQUE(athlete_id, provider)} for future multi-provider rows.
 * Application enforces at most one active lifecycle across all providers.
 */
public final class ActiveConnectionPolicy {

	private ActiveConnectionPolicy() {
	}

	public static Optional<Connection> findBlockingActive(
			List<Connection> existing,
			ConnectionId excludingId) {
		Objects.requireNonNull(existing, "existing must not be null");
		return existing.stream()
				.filter(connection -> connection.status().isActive())
				.filter(connection -> excludingId == null || !connection.id().equals(excludingId))
				.findFirst();
	}

	public static void assertMayActivate(
			List<Connection> existing,
			ConnectionId excludingId) {
		findBlockingActive(existing, excludingId).ifPresent(blocker -> {
			throw new IllegalStateException(
					"Athlete already has an active integration connection ("
							+ blocker.provider()
							+ "); disconnect it before connecting another provider");
		});
	}

	public static Optional<Connection> findByProvider(List<Connection> existing, HealthProviderKey provider) {
		Objects.requireNonNull(existing, "existing must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		return existing.stream().filter(connection -> connection.provider() == provider).findFirst();
	}

	public static boolean owns(Connection connection, UUID accountId, UUID athleteId) {
		Objects.requireNonNull(connection, "connection must not be null");
		return connection.accountId().equals(accountId) && connection.athleteId().equals(athleteId);
	}

}
