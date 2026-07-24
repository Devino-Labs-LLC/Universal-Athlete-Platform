package com.devinolabs.uap.athlete.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class Assessment {

	private static final int MAX_TITLE_LENGTH = 160;
	private static final int MAX_DESCRIPTION_LENGTH = 1000;
	private static final int MAX_NOTES_LENGTH = 2000;
	private static final int MAX_CUSTOM_TYPE_NAME_LENGTH = 120;

	private final AssessmentId id;
	private final AthleteId athleteId;
	private AthleteSportId athleteSportId;
	private AthleteGoalId athleteGoalId;
	private final AssessmentType type;
	private final String customTypeName;
	private String title;
	private String normalizedTitle;
	private String description;
	private AssessmentStatus status;
	private Instant scheduledAt;
	private Instant startedAt;
	private Instant completedAt;
	private String notes;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Assessment(
			AssessmentId id,
			AthleteId athleteId,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			AssessmentType type,
			String customTypeName,
			String title,
			String normalizedTitle,
			String description,
			AssessmentStatus status,
			Instant scheduledAt,
			Instant startedAt,
			Instant completedAt,
			String notes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.customTypeName = normalizeCustomTypeName(type, customTypeName);
		this.title = requireTitle(title);
		this.normalizedTitle = Objects.requireNonNull(normalizedTitle, "normalizedTitle must not be null");
		this.description = normalizeOptionalText(description, MAX_DESCRIPTION_LENGTH, "description");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.scheduledAt = scheduledAt;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.notes = normalizeOptionalText(notes, MAX_NOTES_LENGTH, "notes");
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		enforceStatusTimestamps();
	}

	public static Assessment create(
			AssessmentId id,
			AthleteId athleteId,
			AssessmentType type,
			String customTypeName,
			String title,
			String description,
			Instant scheduledAt,
			String notes,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Assessment(
				id,
				athleteId,
				athleteSportId,
				athleteGoalId,
				type,
				customTypeName,
				title,
				normalizeTitle(title),
				description,
				AssessmentStatus.PLANNED,
				scheduledAt,
				null,
				null,
				notes,
				now,
				now,
				0L);
	}

	public static Assessment rehydrate(
			AssessmentId id,
			AthleteId athleteId,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			AssessmentType type,
			String customTypeName,
			String title,
			String normalizedTitle,
			String description,
			AssessmentStatus status,
			Instant scheduledAt,
			Instant startedAt,
			Instant completedAt,
			String notes,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Assessment(
				id,
				athleteId,
				athleteSportId,
				athleteGoalId,
				type,
				customTypeName,
				title,
				normalizedTitle,
				description,
				status,
				scheduledAt,
				startedAt,
				completedAt,
				notes,
				createdAt,
				updatedAt,
				version);
	}

	public void schedule(Instant scheduledAt, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.scheduledAt = scheduledAt;
		touch(clock);
	}

	public void start(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AssessmentStatus.IN_PROGRESS) {
			return;
		}
		if (status != AssessmentStatus.PLANNED) {
			throw new IllegalStateException("Only PLANNED assessments can be started");
		}
		this.status = AssessmentStatus.IN_PROGRESS;
		this.startedAt = Instant.now(clock);
		this.completedAt = null;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AssessmentStatus.COMPLETED) {
			return;
		}
		if (status != AssessmentStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only IN_PROGRESS assessments can be completed");
		}
		Instant now = Instant.now(clock);
		if (startedAt != null && now.isBefore(startedAt)) {
			throw new IllegalArgumentException("completedAt cannot be before startedAt");
		}
		this.status = AssessmentStatus.COMPLETED;
		this.completedAt = now;
		touch(clock);
	}

	public void cancel(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AssessmentStatus.CANCELLED) {
			return;
		}
		if (status != AssessmentStatus.PLANNED && status != AssessmentStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only PLANNED or IN_PROGRESS assessments can be cancelled");
		}
		this.status = AssessmentStatus.CANCELLED;
		this.completedAt = null;
		touch(clock);
	}

	public void reopen(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AssessmentStatus.COMPLETED) {
			this.status = AssessmentStatus.IN_PROGRESS;
			this.completedAt = null;
			touch(clock);
			return;
		}
		if (status == AssessmentStatus.CANCELLED) {
			this.status = AssessmentStatus.PLANNED;
			this.startedAt = null;
			this.completedAt = null;
			touch(clock);
			return;
		}
		if (status == AssessmentStatus.PLANNED || status == AssessmentStatus.IN_PROGRESS) {
			return;
		}
		throw new IllegalStateException("Assessment cannot be reopened from status " + status);
	}

	public void applyStatusAction(AssessmentStatusAction action, Clock clock) {
		Objects.requireNonNull(action, "action must not be null");
		switch (action) {
			case START -> start(clock);
			case COMPLETE -> complete(clock);
			case CANCEL -> cancel(clock);
			case REOPEN -> reopen(clock);
		}
	}

	public void rename(String title, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.title = requireTitle(title);
		this.normalizedTitle = normalizeTitle(title);
		touch(clock);
	}

	public void changeDescription(String description, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.description = normalizeOptionalText(description, MAX_DESCRIPTION_LENGTH, "description");
		touch(clock);
	}

	public void changeNotes(String notes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.notes = normalizeOptionalText(notes, MAX_NOTES_LENGTH, "notes");
		touch(clock);
	}

	public void linkSport(AthleteSportId athleteSportId, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.athleteSportId = Objects.requireNonNull(athleteSportId, "athleteSportId must not be null");
		touch(clock);
	}

	public void unlinkSport(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.athleteSportId = null;
		touch(clock);
	}

	public void linkGoal(AthleteGoalId athleteGoalId, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.athleteGoalId = Objects.requireNonNull(athleteGoalId, "athleteGoalId must not be null");
		touch(clock);
	}

	public void unlinkGoal(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.athleteGoalId = null;
		touch(clock);
	}

	public boolean isDuplicateCandidate() {
		return status != AssessmentStatus.CANCELLED;
	}

	public static String normalizeTitle(String title) {
		return collapseWhitespace(requireTitle(title)).toLowerCase(Locale.ROOT);
	}

	private void enforceStatusTimestamps() {
		switch (status) {
			case PLANNED -> {
				if (startedAt != null || completedAt != null) {
					throw new IllegalArgumentException("PLANNED assessments cannot have startedAt or completedAt");
				}
			}
			case IN_PROGRESS -> {
				if (startedAt == null) {
					throw new IllegalArgumentException("IN_PROGRESS assessments require startedAt");
				}
				if (completedAt != null) {
					throw new IllegalArgumentException("IN_PROGRESS assessments cannot have completedAt");
				}
			}
			case COMPLETED -> {
				if (startedAt == null || completedAt == null) {
					throw new IllegalArgumentException("COMPLETED assessments require startedAt and completedAt");
				}
				if (completedAt.isBefore(startedAt)) {
					throw new IllegalArgumentException("completedAt cannot be before startedAt");
				}
			}
			case CANCELLED -> {
				if (completedAt != null) {
					throw new IllegalArgumentException("CANCELLED assessments cannot have completedAt");
				}
			}
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
		enforceStatusTimestamps();
	}

	private static String normalizeCustomTypeName(AssessmentType type, String customTypeName) {
		if (type == AssessmentType.OTHER) {
			if (customTypeName == null || customTypeName.isBlank()) {
				throw new IllegalArgumentException("customTypeName is required when assessment type is OTHER");
			}
			String normalized = customTypeName.trim();
			if (normalized.length() > MAX_CUSTOM_TYPE_NAME_LENGTH) {
				throw new IllegalArgumentException(
						"customTypeName must not exceed " + MAX_CUSTOM_TYPE_NAME_LENGTH + " characters");
			}
			return normalized;
		}
		if (customTypeName != null && !customTypeName.isBlank()) {
			throw new IllegalArgumentException("customTypeName must be absent unless assessment type is OTHER");
		}
		return null;
	}

	private static String requireTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title must not be blank");
		}
		String normalized = title.trim();
		if (normalized.length() > MAX_TITLE_LENGTH) {
			throw new IllegalArgumentException("title must not exceed " + MAX_TITLE_LENGTH + " characters");
		}
		return normalized;
	}

	private static String collapseWhitespace(String value) {
		return value.replaceAll("\\s+", " ");
	}

	private static String normalizeOptionalText(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	public AssessmentId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public AthleteSportId athleteSportId() {
		return athleteSportId;
	}

	public AthleteGoalId athleteGoalId() {
		return athleteGoalId;
	}

	public AssessmentType type() {
		return type;
	}

	public String customTypeName() {
		return customTypeName;
	}

	public String title() {
		return title;
	}

	public String normalizedTitle() {
		return normalizedTitle;
	}

	public String description() {
		return description;
	}

	public AssessmentStatus status() {
		return status;
	}

	public Instant scheduledAt() {
		return scheduledAt;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant completedAt() {
		return completedAt;
	}

	public String notes() {
		return notes;
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
