package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Athlete-owned coach assignment. Distinct from recommendation, plan, and execution.
 */
public class TrainingAssignment {

	public static final int MAX_TITLE_LENGTH = 160;
	public static final int MAX_DESCRIPTION_LENGTH = 2000;
	public static final int MAX_RESPONSE_NOTE_LENGTH = 500;
	public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 80;

	private final UUID id;
	private final UUID athleteId;
	private final UUID teamId;
	private final UUID organizationId;
	private final UUID athleteMembershipId;
	private final UUID assignedByAccountId;
	private final String assignedByRole;
	private String title;
	private String description;
	private LocalDate scheduledDate;
	private TrainingAssignmentStatus status;
	private String athleteResponseNote;
	private Instant respondedAt;
	private final String idempotencyKey;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private TrainingAssignment(
			UUID id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID athleteMembershipId,
			UUID assignedByAccountId,
			String assignedByRole,
			String title,
			String description,
			LocalDate scheduledDate,
			TrainingAssignmentStatus status,
			String athleteResponseNote,
			Instant respondedAt,
			String idempotencyKey,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
		this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
		this.athleteMembershipId = Objects.requireNonNull(athleteMembershipId, "athleteMembershipId must not be null");
		this.assignedByAccountId = Objects.requireNonNull(assignedByAccountId, "assignedByAccountId must not be null");
		this.assignedByRole = Objects.requireNonNull(assignedByRole, "assignedByRole must not be null");
		this.title = requireTitle(title);
		this.description = normalizeOptional(description, MAX_DESCRIPTION_LENGTH, "description");
		this.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.athleteResponseNote = normalizeOptional(athleteResponseNote, MAX_RESPONSE_NOTE_LENGTH, "athleteResponseNote");
		this.respondedAt = respondedAt;
		this.idempotencyKey = requireIdempotencyKey(idempotencyKey);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		this.version = version;
	}

	public static TrainingAssignment assign(
			UUID id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID athleteMembershipId,
			UUID assignedByAccountId,
			String assignedByRole,
			String title,
			String description,
			LocalDate scheduledDate,
			String idempotencyKey,
			Clock clock) {
		Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		return new TrainingAssignment(
				id,
				athleteId,
				teamId,
				organizationId,
				athleteMembershipId,
				assignedByAccountId,
				assignedByRole,
				title,
				description,
				scheduledDate,
				TrainingAssignmentStatus.ASSIGNED,
				null,
				null,
				idempotencyKey,
				now,
				now,
				0L);
	}

	public static TrainingAssignment rehydrate(
			UUID id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID athleteMembershipId,
			UUID assignedByAccountId,
			String assignedByRole,
			String title,
			String description,
			LocalDate scheduledDate,
			TrainingAssignmentStatus status,
			String athleteResponseNote,
			Instant respondedAt,
			String idempotencyKey,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new TrainingAssignment(
				id,
				athleteId,
				teamId,
				organizationId,
				athleteMembershipId,
				assignedByAccountId,
				assignedByRole,
				title,
				description,
				scheduledDate,
				status,
				athleteResponseNote,
				respondedAt,
				idempotencyKey,
				createdAt,
				updatedAt,
				version);
	}

	public void updateContent(long expectedVersion, String title, String description, LocalDate scheduledDate, Clock clock) {
		requireExpectedVersion(expectedVersion);
		requireAssignable();
		this.title = requireTitle(title);
		this.description = normalizeOptional(description, MAX_DESCRIPTION_LENGTH, "description");
		this.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		touch(clock);
	}

	public void decline(String note, Clock clock) {
		respond(TrainingAssignmentStatus.DECLINED, note, clock);
	}

	public void markUnable(String note, Clock clock) {
		respond(TrainingAssignmentStatus.UNABLE, note, clock);
	}

	public boolean sameIntent(String title, String description, LocalDate scheduledDate) {
		return this.title.equals(requireTitle(title))
				&& Objects.equals(this.description, normalizeOptional(description, MAX_DESCRIPTION_LENGTH, "description"))
				&& this.scheduledDate.equals(scheduledDate);
	}

	public boolean isTerminal() {
		return status != TrainingAssignmentStatus.ASSIGNED;
	}

	private void respond(TrainingAssignmentStatus next, String note, Clock clock) {
		String normalized = normalizeOptional(note, MAX_RESPONSE_NOTE_LENGTH, "athleteResponseNote");
		if (status == next && Objects.equals(athleteResponseNote, normalized)) {
			return;
		}
		if (status != TrainingAssignmentStatus.ASSIGNED) {
			throw new IllegalStateException("Assignment response is already recorded");
		}
		this.status = next;
		this.athleteResponseNote = normalized;
		this.respondedAt = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		touch(clock);
	}

	private void requireAssignable() {
		if (status != TrainingAssignmentStatus.ASSIGNED) {
			throw new IllegalStateException("Assignment is no longer editable");
		}
	}

	private void requireExpectedVersion(long expectedVersion) {
		if (expectedVersion != version) {
			throw new IllegalStateException("Assignment version does not match");
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
	}

	public void assignPersistedVersion(long version) {
		this.version = version;
	}

	private static String requireTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title must not be blank");
		}
		String trimmed = title.trim();
		if (trimmed.length() > MAX_TITLE_LENGTH) {
			throw new IllegalArgumentException("title is too long");
		}
		return trimmed;
	}

	private static String requireIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("idempotencyKey must not be blank");
		}
		String trimmed = idempotencyKey.trim();
		if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
			throw new IllegalArgumentException("idempotencyKey is too long");
		}
		return trimmed;
	}

	private static String normalizeOptional(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(field + " is too long");
		}
		return trimmed;
	}

	public UUID id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public UUID teamId() {
		return teamId;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public UUID athleteMembershipId() {
		return athleteMembershipId;
	}

	public UUID assignedByAccountId() {
		return assignedByAccountId;
	}

	public String assignedByRole() {
		return assignedByRole;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	public LocalDate scheduledDate() {
		return scheduledDate;
	}

	public TrainingAssignmentStatus status() {
		return status;
	}

	public String athleteResponseNote() {
		return athleteResponseNote;
	}

	public Instant respondedAt() {
		return respondedAt;
	}

	public String idempotencyKey() {
		return idempotencyKey;
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
