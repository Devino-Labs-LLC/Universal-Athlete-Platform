package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

public class WorkoutDay {

	private static final int MAX_TITLE_LENGTH = 160;
	private static final int MAX_DESCRIPTION_LENGTH = 2000;

	private final WorkoutDayId id;
	private final TrainingPlanId trainingPlanId;
	private final AthleteId athleteId;
	private TrainingEnvironmentId trainingEnvironmentOverrideId;
	private int displayOrder;
	private String title;
	private String normalizedTitle;
	private String description;
	private Integer planWeekNumber;
	private DayOfWeek scheduledDayOfWeek;
	private LocalTime plannedStartTime;
	private Integer expectedDurationMinutes;
	private WorkoutDayStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutDay(
			WorkoutDayId id,
			TrainingPlanId trainingPlanId,
			AthleteId athleteId,
			TrainingEnvironmentId trainingEnvironmentOverrideId,
			int displayOrder,
			String title,
			String normalizedTitle,
			String description,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			Integer expectedDurationMinutes,
			WorkoutDayStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.trainingEnvironmentOverrideId = trainingEnvironmentOverrideId;
		this.displayOrder = requireDisplayOrder(displayOrder);
		this.title = requireTitle(title);
		this.normalizedTitle = Objects.requireNonNull(normalizedTitle, "normalizedTitle must not be null");
		this.description = normalizeDescription(description);
		this.planWeekNumber = normalizePlanWeekNumber(planWeekNumber);
		this.scheduledDayOfWeek = Objects.requireNonNull(scheduledDayOfWeek, "scheduledDayOfWeek must not be null");
		this.plannedStartTime = plannedStartTime;
		this.expectedDurationMinutes = normalizeDuration(expectedDurationMinutes);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutDay create(
			WorkoutDayId id,
			TrainingPlanId trainingPlanId,
			AthleteId athleteId,
			int displayOrder,
			String title,
			String description,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			Integer expectedDurationMinutes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutDay(
				id,
				trainingPlanId,
				athleteId,
				null,
				displayOrder,
				title,
				normalizeTitle(title),
				description,
				planWeekNumber,
				scheduledDayOfWeek,
				plannedStartTime,
				expectedDurationMinutes,
				WorkoutDayStatus.PLANNED,
				now,
				now,
				0L);
	}

	public static WorkoutDay rehydrate(
			WorkoutDayId id,
			TrainingPlanId trainingPlanId,
			AthleteId athleteId,
			TrainingEnvironmentId trainingEnvironmentOverrideId,
			int displayOrder,
			String title,
			String normalizedTitle,
			String description,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			Integer expectedDurationMinutes,
			WorkoutDayStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutDay(
				id,
				trainingPlanId,
				athleteId,
				trainingEnvironmentOverrideId,
				displayOrder,
				title,
				normalizedTitle,
				description,
				planWeekNumber,
				scheduledDayOfWeek,
				plannedStartTime,
				expectedDurationMinutes,
				status,
				createdAt,
				updatedAt,
				version);
	}

	public void changeDisplayOrder(int displayOrder, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		touch(clock);
	}

	public void rename(String title, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.title = requireTitle(title);
		this.normalizedTitle = normalizeTitle(title);
		touch(clock);
	}

	public void changeDescription(String description, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.description = normalizeDescription(description);
		touch(clock);
	}

	public void changePlanWeekNumber(Integer planWeekNumber, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.planWeekNumber = normalizePlanWeekNumber(planWeekNumber);
		touch(clock);
	}

	public void changeScheduledDayOfWeek(DayOfWeek scheduledDayOfWeek, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.scheduledDayOfWeek = Objects.requireNonNull(
				scheduledDayOfWeek, "scheduledDayOfWeek must not be null");
		touch(clock);
	}

	public void changePlannedStartTime(LocalTime plannedStartTime, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.plannedStartTime = plannedStartTime;
		touch(clock);
	}

	public void changeExpectedDurationMinutes(Integer expectedDurationMinutes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.expectedDurationMinutes = normalizeDuration(expectedDurationMinutes);
		touch(clock);
	}

	public void linkTrainingEnvironmentOverride(TrainingEnvironmentId environmentId, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.trainingEnvironmentOverrideId = Objects.requireNonNull(
				environmentId, "trainingEnvironmentOverrideId must not be null");
		touch(clock);
	}

	public void unlinkTrainingEnvironmentOverride(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.trainingEnvironmentOverrideId = null;
		touch(clock);
	}

	public void activate(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutDayStatus.ACTIVE) {
			return;
		}
		if (status != WorkoutDayStatus.PLANNED) {
			throw new IllegalStateException("Only PLANNED workout days can be activated");
		}
		this.status = WorkoutDayStatus.ACTIVE;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutDayStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutDayStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE workout days can be completed");
		}
		this.status = WorkoutDayStatus.COMPLETED;
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutDayStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutDayStatus.PLANNED && status != WorkoutDayStatus.ACTIVE) {
			throw new IllegalStateException("Only PLANNED or ACTIVE workout days can be skipped");
		}
		this.status = WorkoutDayStatus.SKIPPED;
		touch(clock);
	}

	public void applyStatusAction(WorkoutDayStatusAction action, Clock clock) {
		Objects.requireNonNull(action, "action must not be null");
		switch (action) {
			case ACTIVATE -> activate(clock);
			case COMPLETE -> complete(clock);
			case SKIP -> skip(clock);
		}
	}

	public boolean hasSchedulablePlacement() {
		return planWeekNumber != null && scheduledDayOfWeek != null;
	}

	public static String normalizeTitle(String title) {
		return collapseWhitespace(requireTitle(title)).toLowerCase(Locale.ROOT);
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static int requireDisplayOrder(int displayOrder) {
		if (displayOrder < 0) {
			throw new IllegalArgumentException("displayOrder must not be negative");
		}
		return displayOrder;
	}

	private static Integer normalizePlanWeekNumber(Integer planWeekNumber) {
		if (planWeekNumber == null) {
			return null;
		}
		if (planWeekNumber < 1) {
			throw new IllegalArgumentException("planWeekNumber must be at least 1");
		}
		return planWeekNumber;
	}

	private static String requireTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title must not be blank");
		}
		String trimmed = title.trim();
		if (trimmed.length() > MAX_TITLE_LENGTH) {
			throw new IllegalArgumentException("title must not exceed " + MAX_TITLE_LENGTH + " characters");
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

	private static Integer normalizeDuration(Integer expectedDurationMinutes) {
		if (expectedDurationMinutes == null) {
			return null;
		}
		if (expectedDurationMinutes <= 0) {
			throw new IllegalArgumentException("expectedDurationMinutes must be positive");
		}
		return expectedDurationMinutes;
	}

	public WorkoutDayId id() {
		return id;
	}

	public TrainingPlanId trainingPlanId() {
		return trainingPlanId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public TrainingEnvironmentId trainingEnvironmentOverrideId() {
		return trainingEnvironmentOverrideId;
	}

	public int displayOrder() {
		return displayOrder;
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

	public Integer planWeekNumber() {
		return planWeekNumber;
	}

	public DayOfWeek scheduledDayOfWeek() {
		return scheduledDayOfWeek;
	}

	public LocalTime plannedStartTime() {
		return plannedStartTime;
	}

	public Integer expectedDurationMinutes() {
		return expectedDurationMinutes;
	}

	public WorkoutDayStatus status() {
		return status;
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
