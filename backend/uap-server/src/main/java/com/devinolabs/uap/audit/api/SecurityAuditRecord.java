package com.devinolabs.uap.audit.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Allow-listed security audit fields only. Never carry tokens, JWTs, passwords,
 * wellness bodies, readiness payloads, or arbitrary request bodies.
 */
public record SecurityAuditRecord(
		UUID id,
		String eventType,
		Instant occurredAt,
		UUID actorAccountId,
		UUID subjectAccountId,
		UUID subjectAthleteId,
		UUID organizationId,
		UUID teamId,
		String resourceType,
		UUID resourceId,
		String metadataJson) {

	public SecurityAuditRecord {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		if (eventType.isBlank() || eventType.length() > 64) {
			throw new IllegalArgumentException("eventType must be 1..64 characters");
		}
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		if (resourceType != null && (resourceType.isBlank() || resourceType.length() > 40)) {
			throw new IllegalArgumentException("resourceType must be 1..40 characters when present");
		}
		if (metadataJson != null && metadataJson.length() > 500) {
			throw new IllegalArgumentException("metadataJson must be at most 500 characters");
		}
		assertNoForbiddenMetadata(metadataJson);
	}

	public static SecurityAuditRecord of(
			String eventType,
			UUID actorAccountId,
			UUID subjectAccountId,
			UUID subjectAthleteId,
			UUID organizationId,
			UUID teamId,
			String resourceType,
			UUID resourceId,
			String metadataJson) {
		return new SecurityAuditRecord(
				UUID.randomUUID(),
				eventType,
				Instant.now(),
				actorAccountId,
				subjectAccountId,
				subjectAthleteId,
				organizationId,
				teamId,
				resourceType,
				resourceId,
				metadataJson);
	}

	private static void assertNoForbiddenMetadata(String metadataJson) {
		if (metadataJson == null || metadataJson.isBlank()) {
			return;
		}
		String lower = metadataJson.toLowerCase();
		String[] forbidden = {
				"password",
				"token",
				"jwt",
				"bearer",
				"csrf",
				"rawtoken",
				"authorization",
				"secret",
				"credential"
		};
		for (String needle : forbidden) {
			if (lower.contains(needle)) {
				throw new IllegalArgumentException("metadataJson must not contain forbidden audit content");
			}
		}
	}

}
