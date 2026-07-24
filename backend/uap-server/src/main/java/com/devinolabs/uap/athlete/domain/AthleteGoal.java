package com.devinolabs.uap.athlete.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;

public class AthleteGoal {

	private static final int MAX_TITLE_LENGTH = 160;
	private static final int MAX_DESCRIPTION_LENGTH = 1000;
	private static final int MAX_CUSTOM_GOAL_NAME_LENGTH = 120;

	private final AthleteGoalId id;
	private final AthleteId athleteId;
	private final GoalType goalType;
	private final String customGoalName;
	private String title;
	private String normalizedTitle;
	private String description;
	private GoalPriority priority;
	private GoalStatus status;
	private GoalTarget target;
	private LocalDate targetDate;
	private AthleteSportId athleteSportId;
	private final Instant createdAt;
	private Instant updatedAt;
	private Instant completedAt;
	private long version;

	private AthleteGoal(
			AthleteGoalId id,
			AthleteId athleteId,
			GoalType goalType,
			String customGoalName,
			String title,
			String normalizedTitle,
			String description,
			GoalPriority priority,
			GoalStatus status,
			GoalTarget target,
			LocalDate targetDate,
			AthleteSportId athleteSportId,
			Instant createdAt,
			Instant updatedAt,
			Instant completedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "AthleteGoal id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "AthleteGoal athleteId must not be null");
		this.goalType = Objects.requireNonNull(goalType, "AthleteGoal goalType must not be null");
		validateCustomGoalName(goalType, customGoalName);
		this.customGoalName = customGoalName == null || customGoalName.isBlank() ? null : customGoalName.trim();
		this.title = requireTitle(title);
		this.normalizedTitle = Objects.requireNonNull(normalizedTitle, "normalizedTitle must not be null");
		this.description = normalizeDescription(description);
		this.priority = Objects.requireNonNull(priority, "priority must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.target = target;
		this.targetDate = targetDate;
		this.athleteSportId = athleteSportId;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.completedAt = completedAt;
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		enforceCompletedAtInvariant();
		enforceTargetDateInvariant();
	}

	public static AthleteGoal create(
			AthleteGoalId id,
			AthleteId athleteId,
			GoalType goalType,
			String customGoalName,
			String title,
			String description,
			GoalPriority priority,
			GoalTarget target,
			LocalDate targetDate,
			AthleteSportId athleteSportId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		String normalizedTitle = normalizeTitle(title);
		GoalPriority effectivePriority = priority == null ? GoalPriority.MEDIUM : priority;
		return new AthleteGoal(
				id,
				athleteId,
				goalType,
				customGoalName,
				title,
				normalizedTitle,
				description,
				effectivePriority,
				GoalStatus.ACTIVE,
				target,
				targetDate,
				athleteSportId,
				now,
				now,
				null,
				0L);
	}

	public static AthleteGoal rehydrate(
			AthleteGoalId id,
			AthleteId athleteId,
			GoalType goalType,
			String customGoalName,
			String title,
			String normalizedTitle,
			String description,
			GoalPriority priority,
			GoalStatus status,
			GoalTarget target,
			LocalDate targetDate,
			AthleteSportId athleteSportId,
			Instant createdAt,
			Instant updatedAt,
			Instant completedAt,
			long version) {
		return new AthleteGoal(
				id,
				athleteId,
				goalType,
				customGoalName,
				title,
				normalizedTitle,
				description,
				priority,
				status,
				target,
				targetDate,
				athleteSportId,
				createdAt,
				updatedAt,
				completedAt,
				version);
	}

	public void updateDetails(String title, String description, GoalPriority priority, Clock clock) {
		requireEditable(clock);
		this.title = requireTitle(title);
		this.normalizedTitle = normalizeTitle(title);
		this.description = normalizeDescription(description);
		this.priority = Objects.requireNonNull(priority, "priority must not be null");
		touch(clock);
	}

	public void updateTarget(GoalTarget target, Clock clock) {
		requireEditable(clock);
		this.target = target;
		touch(clock);
	}

	public void updateTargetDate(LocalDate targetDate, Clock clock) {
		requireEditable(clock);
		this.targetDate = targetDate;
		enforceTargetDateInvariant();
		touch(clock);
	}

	public void changePriority(GoalPriority priority, Clock clock) {
		requireEditable(clock);
		this.priority = Objects.requireNonNull(priority, "priority must not be null");
		touch(clock);
	}

	public void linkSport(AthleteSportId athleteSportId, Clock clock) {
		requireEditable(clock);
		this.athleteSportId = Objects.requireNonNull(athleteSportId, "athleteSportId must not be null");
		touch(clock);
	}

	public void unlinkSport(Clock clock) {
		requireEditable(clock);
		this.athleteSportId = null;
		touch(clock);
	}

	public void applyStatusAction(GoalStatusAction action, Clock clock) {
		Objects.requireNonNull(action, "action must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		switch (action) {
			case PAUSE -> pause(clock);
			case RESUME -> resume(clock);
			case COMPLETE -> complete(clock);
			case CANCEL -> cancel(clock);
			case REOPEN -> reopen(clock);
		}
	}

	public void pause(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.PAUSED) {
			return;
		}
		if (status != GoalStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE goals can be paused");
		}
		this.status = GoalStatus.PAUSED;
		this.completedAt = null;
		touch(clock);
	}

	public void resume(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.ACTIVE) {
			return;
		}
		if (status != GoalStatus.PAUSED) {
			throw new IllegalStateException("Only PAUSED goals can be resumed");
		}
		this.status = GoalStatus.ACTIVE;
		this.completedAt = null;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.COMPLETED) {
			return;
		}
		if (status != GoalStatus.ACTIVE && status != GoalStatus.PAUSED) {
			throw new IllegalStateException("Only ACTIVE or PAUSED goals can be completed");
		}
		this.status = GoalStatus.COMPLETED;
		this.completedAt = Instant.now(clock);
		touch(clock);
	}

	public void cancel(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.CANCELLED) {
			return;
		}
		if (status == GoalStatus.COMPLETED) {
			throw new IllegalStateException("COMPLETED goals must be reopened before cancelling");
		}
		if (status != GoalStatus.ACTIVE && status != GoalStatus.PAUSED) {
			throw new IllegalStateException("Only ACTIVE or PAUSED goals can be cancelled");
		}
		this.status = GoalStatus.CANCELLED;
		this.completedAt = null;
		touch(clock);
	}

	public void reopen(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.ACTIVE) {
			return;
		}
		if (status != GoalStatus.COMPLETED && status != GoalStatus.CANCELLED) {
			throw new IllegalStateException("Only COMPLETED or CANCELLED goals can be reopened");
		}
		this.status = GoalStatus.ACTIVE;
		this.completedAt = null;
		touch(clock);
	}

	public boolean isActiveDuplicateCandidate() {
		return status == GoalStatus.ACTIVE || status == GoalStatus.PAUSED;
	}

	public static String normalizeTitle(String title) {
		return collapseWhitespace(requireTitle(title)).toLowerCase(Locale.ROOT);
	}

	private void requireEditable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == GoalStatus.COMPLETED || status == GoalStatus.CANCELLED) {
			throw new IllegalStateException("COMPLETED or CANCELLED goals must be reopened before editing");
		}
	}

	private void enforceCompletedAtInvariant() {
		if (status == GoalStatus.COMPLETED) {
			if (completedAt == null) {
				throw new IllegalArgumentException("COMPLETED goals require completedAt");
			}
			return;
		}
		if (completedAt != null) {
			throw new IllegalArgumentException(status + " goals cannot have completedAt");
		}
	}

	private void enforceTargetDateInvariant() {
		if (targetDate == null) {
			return;
		}
		LocalDate createdDate = LocalDate.ofInstant(createdAt, ZoneOffset.UTC);
		if (targetDate.isBefore(createdDate)) {
			throw new IllegalArgumentException("targetDate cannot be before the goal creation date");
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static void validateCustomGoalName(GoalType goalType, String customGoalName) {
		if (goalType == GoalType.OTHER) {
			if (customGoalName == null || customGoalName.isBlank()) {
				throw new IllegalArgumentException("customGoalName is required when goalType is OTHER");
			}
			if (customGoalName.trim().length() > MAX_CUSTOM_GOAL_NAME_LENGTH) {
				throw new IllegalArgumentException(
						"customGoalName must not exceed " + MAX_CUSTOM_GOAL_NAME_LENGTH + " characters");
			}
			return;
		}
		if (customGoalName != null && !customGoalName.isBlank()) {
			throw new IllegalArgumentException("customGoalName must be absent unless goalType is OTHER");
		}
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

	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		String normalized = description.trim();
		if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException(
					"description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return normalized;
	}

	public AthleteGoalId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public GoalType goalType() {
		return goalType;
	}

	public String customGoalName() {
		return customGoalName;
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

	public GoalPriority priority() {
		return priority;
	}

	public GoalStatus status() {
		return status;
	}

	public GoalTarget target() {
		return target;
	}

	public LocalDate targetDate() {
		return targetDate;
	}

	public AthleteSportId athleteSportId() {
		return athleteSportId;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public Instant completedAt() {
		return completedAt;
	}

	public long version() {
		return version;
	}

}
