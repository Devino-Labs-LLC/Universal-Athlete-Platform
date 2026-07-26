package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * A training plan owned by an athlete.
 *
 * <p>Two independent lifecycles live on this aggregate. {@link TrainingPlanStatus} governs
 * ownership and content (draft, active, completed, archived), while
 * {@link TrainingPlanScheduleStatus} governs calendar activation: whether placements may be
 * materialised into dated occurrences.
 */
public class TrainingPlan {

	private static final int MAX_NAME_LENGTH = 160;
	private static final int MAX_DESCRIPTION_LENGTH = 2000;
	private static final int MAX_CUSTOM_TYPE_NAME_LENGTH = 120;

	private final TrainingPlanId id;
	private final AthleteId athleteId;
	private AthleteSportId athleteSportId;
	private AthleteGoalId athleteGoalId;
	private String name;
	private String normalizedName;
	private String description;
	private final TrainingPlanType type;
	private final String customTypeName;
	private TrainingPlanStatus status;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate scheduleStartDate;
	private LocalDate scheduleEndDate;
	private String scheduleTimezone;
	private TrainingPlanScheduleStatus scheduleStatus;
	private TrainingPlanRecurrenceMode recurrenceMode;
	private LocalDate scheduleGeneratedThrough;
	private Instant scheduleActivatedAt;
	private Instant schedulePausedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private TrainingPlan(
			TrainingPlanId id,
			AthleteId athleteId,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			String name,
			String normalizedName,
			String description,
			TrainingPlanType type,
			String customTypeName,
			TrainingPlanStatus status,
			LocalDate startDate,
			LocalDate endDate,
			LocalDate scheduleStartDate,
			LocalDate scheduleEndDate,
			String scheduleTimezone,
			TrainingPlanScheduleStatus scheduleStatus,
			TrainingPlanRecurrenceMode recurrenceMode,
			LocalDate scheduleGeneratedThrough,
			Instant scheduleActivatedAt,
			Instant schedulePausedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.customTypeName = normalizeCustomTypeName(type, customTypeName);
		this.name = requireName(name);
		this.normalizedName = Objects.requireNonNull(normalizedName, "normalizedName must not be null");
		this.description = normalizeDescription(description);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
		this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
		enforceDateRange(this.startDate, this.endDate);
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.scheduleStartDate = scheduleStartDate;
		this.scheduleEndDate = scheduleEndDate;
		this.scheduleTimezone = scheduleTimezone;
		this.scheduleStatus = Objects.requireNonNull(scheduleStatus, "scheduleStatus must not be null");
		this.recurrenceMode = recurrenceMode;
		this.scheduleGeneratedThrough = scheduleGeneratedThrough;
		this.scheduleActivatedAt = scheduleActivatedAt;
		this.schedulePausedAt = schedulePausedAt;
		enforceScheduleDateRange(scheduleStartDate, scheduleEndDate);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static TrainingPlan create(
			TrainingPlanId id,
			AthleteId athleteId,
			TrainingPlanType type,
			String customTypeName,
			String name,
			String description,
			LocalDate startDate,
			LocalDate endDate,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new TrainingPlan(
				id,
				athleteId,
				athleteSportId,
				athleteGoalId,
				name,
				normalizeName(name),
				description,
				type,
				customTypeName,
				TrainingPlanStatus.DRAFT,
				startDate,
				endDate,
				null,
				null,
				null,
				TrainingPlanScheduleStatus.DRAFT,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static TrainingPlan rehydrate(
			TrainingPlanId id,
			AthleteId athleteId,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			String name,
			String normalizedName,
			String description,
			TrainingPlanType type,
			String customTypeName,
			TrainingPlanStatus status,
			LocalDate startDate,
			LocalDate endDate,
			LocalDate scheduleStartDate,
			LocalDate scheduleEndDate,
			String scheduleTimezone,
			TrainingPlanScheduleStatus scheduleStatus,
			TrainingPlanRecurrenceMode recurrenceMode,
			LocalDate scheduleGeneratedThrough,
			Instant scheduleActivatedAt,
			Instant schedulePausedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new TrainingPlan(
				id,
				athleteId,
				athleteSportId,
				athleteGoalId,
				name,
				normalizedName,
				description,
				type,
				customTypeName,
				status,
				startDate,
				endDate,
				scheduleStartDate,
				scheduleEndDate,
				scheduleTimezone,
				scheduleStatus,
				recurrenceMode,
				scheduleGeneratedThrough,
				scheduleActivatedAt,
				schedulePausedAt,
				createdAt,
				updatedAt,
				version);
	}

	public void rename(String name, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.name = requireName(name);
		this.normalizedName = normalizeName(name);
		touch(clock);
	}

	public void changeDescription(String description, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.description = normalizeDescription(description);
		touch(clock);
	}

	public void changeDates(LocalDate startDate, LocalDate endDate, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		LocalDate effectiveStart = startDate == null ? this.startDate : startDate;
		LocalDate effectiveEnd = endDate == null ? this.endDate : endDate;
		Objects.requireNonNull(effectiveStart, "startDate must not be null");
		Objects.requireNonNull(effectiveEnd, "endDate must not be null");
		enforceDateRange(effectiveStart, effectiveEnd);
		this.startDate = effectiveStart;
		this.endDate = effectiveEnd;
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

	public void activate(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == TrainingPlanStatus.ACTIVE) {
			return;
		}
		if (status != TrainingPlanStatus.DRAFT) {
			throw new IllegalStateException("Only DRAFT plans can be activated");
		}
		this.status = TrainingPlanStatus.ACTIVE;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == TrainingPlanStatus.COMPLETED) {
			return;
		}
		if (status != TrainingPlanStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE plans can be completed");
		}
		this.status = TrainingPlanStatus.COMPLETED;
		touch(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == TrainingPlanStatus.ARCHIVED) {
			return;
		}
		if (status != TrainingPlanStatus.DRAFT
				&& status != TrainingPlanStatus.ACTIVE
				&& status != TrainingPlanStatus.COMPLETED) {
			throw new IllegalStateException("Plan cannot be archived from status " + status);
		}
		this.status = TrainingPlanStatus.ARCHIVED;
		touch(clock);
	}

	public void applyStatusAction(TrainingPlanStatusAction action, Clock clock) {
		Objects.requireNonNull(action, "action must not be null");
		switch (action) {
			case ACTIVATE -> activate(clock);
			case COMPLETE -> complete(clock);
			case ARCHIVE -> archive(clock);
		}
	}

	public void activateSchedule(
			LocalDate scheduleStartDate,
			LocalDate scheduleEndDate,
			String scheduleTimezone,
			TrainingPlanRecurrenceMode recurrenceMode,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(scheduleStartDate, "scheduleStartDate must not be null");
		Objects.requireNonNull(recurrenceMode, "recurrenceMode must not be null");
		if (scheduleTimezone == null || scheduleTimezone.isBlank()) {
			throw new IllegalArgumentException("scheduleTimezone must not be blank");
		}
		if (scheduleStatus != TrainingPlanScheduleStatus.DRAFT) {
			throw new IllegalStateException("Only DRAFT schedules can be activated");
		}
		enforceScheduleDateRange(scheduleStartDate, scheduleEndDate);
		this.scheduleStartDate = scheduleStartDate;
		this.scheduleEndDate = scheduleEndDate;
		this.scheduleTimezone = scheduleTimezone.trim();
		this.recurrenceMode = recurrenceMode;
		this.scheduleStatus = TrainingPlanScheduleStatus.ACTIVE;
		this.scheduleActivatedAt = Instant.now(clock);
		this.schedulePausedAt = null;
		touch(clock);
	}

	public void pauseSchedule(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (scheduleStatus == TrainingPlanScheduleStatus.PAUSED) {
			return;
		}
		if (scheduleStatus != TrainingPlanScheduleStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE schedules can be paused");
		}
		this.scheduleStatus = TrainingPlanScheduleStatus.PAUSED;
		this.schedulePausedAt = Instant.now(clock);
		touch(clock);
	}

	public void resumeSchedule(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (scheduleStatus == TrainingPlanScheduleStatus.ACTIVE) {
			return;
		}
		if (scheduleStatus != TrainingPlanScheduleStatus.PAUSED) {
			throw new IllegalStateException("Only PAUSED schedules can be resumed");
		}
		this.scheduleStatus = TrainingPlanScheduleStatus.ACTIVE;
		this.schedulePausedAt = null;
		touch(clock);
	}

	public void completeSchedule(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (scheduleStatus == TrainingPlanScheduleStatus.COMPLETED) {
			return;
		}
		if (scheduleStatus != TrainingPlanScheduleStatus.ACTIVE
				&& scheduleStatus != TrainingPlanScheduleStatus.PAUSED) {
			throw new IllegalStateException("Only ACTIVE or PAUSED schedules can be completed");
		}
		this.scheduleStatus = TrainingPlanScheduleStatus.COMPLETED;
		touch(clock);
	}

	/**
	 * Moves the generation watermark forward, clamped to {@code scheduleEndDate} and to
	 * {@code maximumDate} (the last placement date a FINITE plan can ever produce). The watermark
	 * never moves backwards.
	 */
	public void advanceGeneratedThrough(LocalDate candidate, LocalDate maximumDate, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(candidate, "candidate must not be null");
		LocalDate effective = candidate;
		if (scheduleEndDate != null && effective.isAfter(scheduleEndDate)) {
			effective = scheduleEndDate;
		}
		if (maximumDate != null && effective.isAfter(maximumDate)) {
			effective = maximumDate;
		}
		if (scheduleGeneratedThrough != null && !effective.isAfter(scheduleGeneratedThrough)) {
			return;
		}
		this.scheduleGeneratedThrough = effective;
		touch(clock);
	}

	public boolean isScheduleActive() {
		return scheduleStatus == TrainingPlanScheduleStatus.ACTIVE;
	}

	public boolean isDuplicateCandidate() {
		return status != TrainingPlanStatus.ARCHIVED;
	}

	public static String normalizeName(String name) {
		return collapseWhitespace(requireName(name)).toLowerCase(Locale.ROOT);
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static void enforceDateRange(LocalDate startDate, LocalDate endDate) {
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}
	}

	private static void enforceScheduleDateRange(LocalDate scheduleStartDate, LocalDate scheduleEndDate) {
		if (scheduleStartDate != null && scheduleEndDate != null && scheduleEndDate.isBefore(scheduleStartDate)) {
			throw new IllegalArgumentException("scheduleEndDate must not be before scheduleStartDate");
		}
	}

	private static String normalizeCustomTypeName(TrainingPlanType type, String customTypeName) {
		if (type == TrainingPlanType.OTHER) {
			if (customTypeName == null || customTypeName.isBlank()) {
				throw new IllegalArgumentException("customTypeName is required when plan type is OTHER");
			}
			String normalized = customTypeName.trim();
			if (normalized.length() > MAX_CUSTOM_TYPE_NAME_LENGTH) {
				throw new IllegalArgumentException(
						"customTypeName must not exceed " + MAX_CUSTOM_TYPE_NAME_LENGTH + " characters");
			}
			return normalized;
		}
		if (customTypeName != null && !customTypeName.isBlank()) {
			throw new IllegalArgumentException("customTypeName must be absent unless plan type is OTHER");
		}
		return null;
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("name must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
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
			throw new IllegalArgumentException("description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return normalized;
	}

	public TrainingPlanId id() {
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

	public String name() {
		return name;
	}

	public String normalizedName() {
		return normalizedName;
	}

	public String description() {
		return description;
	}

	public TrainingPlanType type() {
		return type;
	}

	public String customTypeName() {
		return customTypeName;
	}

	public TrainingPlanStatus status() {
		return status;
	}

	public LocalDate startDate() {
		return startDate;
	}

	public LocalDate endDate() {
		return endDate;
	}

	public LocalDate scheduleStartDate() {
		return scheduleStartDate;
	}

	public LocalDate scheduleEndDate() {
		return scheduleEndDate;
	}

	public String scheduleTimezone() {
		return scheduleTimezone;
	}

	public TrainingPlanScheduleStatus scheduleStatus() {
		return scheduleStatus;
	}

	public TrainingPlanRecurrenceMode recurrenceMode() {
		return recurrenceMode;
	}

	public LocalDate scheduleGeneratedThrough() {
		return scheduleGeneratedThrough;
	}

	public Instant scheduleActivatedAt() {
		return scheduleActivatedAt;
	}

	public Instant schedulePausedAt() {
		return schedulePausedAt;
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
