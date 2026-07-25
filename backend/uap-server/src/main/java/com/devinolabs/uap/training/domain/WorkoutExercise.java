package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class WorkoutExercise {

	private static final int MAX_NAME_LENGTH = 160;
	private static final int MAX_TEMPO_LENGTH = 40;
	private static final int MAX_COACHING_NOTES_LENGTH = 2000;

	private final WorkoutExerciseId id;
	private final WorkoutDayId workoutDayId;
	private final AthleteId athleteId;
	private int displayOrder;
	private String exerciseName;
	private String normalizedExerciseName;
	private ExerciseCategory category;
	private ExerciseType type;
	private Integer sets;
	private Integer minimumReps;
	private Integer maximumReps;
	private BigDecimal targetWeight;
	private WeightUnit weightUnit;
	private Integer targetDurationSeconds;
	private BigDecimal targetDistance;
	private DistanceUnit distanceUnit;
	private Integer targetRestSeconds;
	private Integer targetRpe;
	private String tempo;
	private String coachingNotes;
	private WorkoutExerciseStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutExercise(
			WorkoutExerciseId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			int displayOrder,
			String exerciseName,
			String normalizedExerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			WorkoutExerciseStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		this.exerciseName = requireExerciseName(exerciseName);
		this.normalizedExerciseName = Objects.requireNonNull(
				normalizedExerciseName, "normalizedExerciseName must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.sets = requireSets(sets);
		Prescription prescription = normalizePrescription(
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				targetDurationSeconds,
				targetDistance,
				distanceUnit,
				targetRestSeconds,
				targetRpe,
				tempo,
				coachingNotes);
		this.minimumReps = prescription.minimumReps();
		this.maximumReps = prescription.maximumReps();
		this.targetWeight = prescription.targetWeight();
		this.weightUnit = prescription.weightUnit();
		this.targetDurationSeconds = prescription.targetDurationSeconds();
		this.targetDistance = prescription.targetDistance();
		this.distanceUnit = prescription.distanceUnit();
		this.targetRestSeconds = prescription.targetRestSeconds();
		this.targetRpe = prescription.targetRpe();
		this.tempo = prescription.tempo();
		this.coachingNotes = prescription.coachingNotes();
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutExercise create(
			WorkoutExerciseId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			int displayOrder,
			String exerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new WorkoutExercise(
				id,
				workoutDayId,
				athleteId,
				displayOrder,
				exerciseName,
				normalizeExerciseName(exerciseName),
				category,
				type,
				sets,
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				targetDurationSeconds,
				targetDistance,
				distanceUnit,
				targetRestSeconds,
				targetRpe,
				tempo,
				coachingNotes,
				WorkoutExerciseStatus.PLANNED,
				now,
				now,
				0L);
	}

	public static WorkoutExercise rehydrate(
			WorkoutExerciseId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			int displayOrder,
			String exerciseName,
			String normalizedExerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			WorkoutExerciseStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutExercise(
				id,
				workoutDayId,
				athleteId,
				displayOrder,
				exerciseName,
				normalizedExerciseName,
				category,
				type,
				sets,
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				targetDurationSeconds,
				targetDistance,
				distanceUnit,
				targetRestSeconds,
				targetRpe,
				tempo,
				coachingNotes,
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

	public void rename(String exerciseName, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.exerciseName = requireExerciseName(exerciseName);
		this.normalizedExerciseName = normalizeExerciseName(exerciseName);
		touch(clock);
	}

	public void changeCategory(ExerciseCategory category, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
		touch(clock);
	}

	public void changeType(ExerciseType type, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		touch(clock);
	}

	public void changeSets(Integer sets, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.sets = requireSets(sets);
		touch(clock);
	}

	public void changePrescription(
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Prescription prescription = normalizePrescription(
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				targetDurationSeconds,
				targetDistance,
				distanceUnit,
				targetRestSeconds,
				targetRpe,
				tempo,
				coachingNotes);
		this.minimumReps = prescription.minimumReps();
		this.maximumReps = prescription.maximumReps();
		this.targetWeight = prescription.targetWeight();
		this.weightUnit = prescription.weightUnit();
		this.targetDurationSeconds = prescription.targetDurationSeconds();
		this.targetDistance = prescription.targetDistance();
		this.distanceUnit = prescription.distanceUnit();
		this.targetRestSeconds = prescription.targetRestSeconds();
		this.targetRpe = prescription.targetRpe();
		this.tempo = prescription.tempo();
		this.coachingNotes = prescription.coachingNotes();
		touch(clock);
	}

	public void changeMinimumReps(Integer minimumReps, Clock clock) {
		changePrescription(
				minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeMaximumReps(Integer maximumReps, Clock clock) {
		changePrescription(
				this.minimumReps,
				maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTargetWeight(BigDecimal targetWeight, WeightUnit weightUnit, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				targetWeight,
				weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTargetDurationSeconds(Integer targetDurationSeconds, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTargetDistance(BigDecimal targetDistance, DistanceUnit distanceUnit, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				targetDistance,
				distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTargetRestSeconds(Integer targetRestSeconds, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				targetRestSeconds,
				this.targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTargetRpe(Integer targetRpe, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				targetRpe,
				this.tempo,
				this.coachingNotes,
				clock);
	}

	public void changeTempo(String tempo, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				tempo,
				this.coachingNotes,
				clock);
	}

	public void changeCoachingNotes(String coachingNotes, Clock clock) {
		changePrescription(
				this.minimumReps,
				this.maximumReps,
				this.targetWeight,
				this.weightUnit,
				this.targetDurationSeconds,
				this.targetDistance,
				this.distanceUnit,
				this.targetRestSeconds,
				this.targetRpe,
				this.tempo,
				coachingNotes,
				clock);
	}

	public void activate(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseStatus.ACTIVE) {
			return;
		}
		if (status != WorkoutExerciseStatus.PLANNED) {
			throw new IllegalStateException("Only PLANNED workout exercises can be activated");
		}
		this.status = WorkoutExerciseStatus.ACTIVE;
		touch(clock);
	}

	public void complete(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseStatus.COMPLETED) {
			return;
		}
		if (status != WorkoutExerciseStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE workout exercises can be completed");
		}
		this.status = WorkoutExerciseStatus.COMPLETED;
		touch(clock);
	}

	public void skip(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == WorkoutExerciseStatus.SKIPPED) {
			return;
		}
		if (status != WorkoutExerciseStatus.PLANNED && status != WorkoutExerciseStatus.ACTIVE) {
			throw new IllegalStateException("Only PLANNED or ACTIVE workout exercises can be skipped");
		}
		this.status = WorkoutExerciseStatus.SKIPPED;
		touch(clock);
	}

	public void applyStatusAction(WorkoutExerciseStatusAction action, Clock clock) {
		Objects.requireNonNull(action, "action must not be null");
		switch (action) {
			case ACTIVATE -> activate(clock);
			case COMPLETE -> complete(clock);
			case SKIP -> skip(clock);
		}
	}

	public static String normalizeExerciseName(String exerciseName) {
		return collapseWhitespace(requireExerciseName(exerciseName)).toLowerCase(Locale.ROOT);
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

	private static String requireExerciseName(String exerciseName) {
		if (exerciseName == null || exerciseName.isBlank()) {
			throw new IllegalArgumentException("exerciseName must not be blank");
		}
		String trimmed = exerciseName.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("exerciseName must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private static Integer requireSets(Integer sets) {
		if (sets == null) {
			throw new IllegalArgumentException("sets must not be null");
		}
		if (sets < 1) {
			throw new IllegalArgumentException("sets must be >= 1");
		}
		return sets;
	}

	private static String collapseWhitespace(String value) {
		return value.replaceAll("\\s+", " ");
	}

	private static Prescription normalizePrescription(
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes) {
		if ((minimumReps == null) != (maximumReps == null)) {
			throw new IllegalArgumentException("minimumReps and maximumReps must both be provided or both omitted");
		}
		if (minimumReps != null) {
			if (minimumReps < 1) {
				throw new IllegalArgumentException("minimumReps must be >= 1");
			}
			if (maximumReps < minimumReps) {
				throw new IllegalArgumentException("maximumReps must be >= minimumReps");
			}
		}
		if ((targetWeight == null) != (weightUnit == null)) {
			throw new IllegalArgumentException("targetWeight and weightUnit must both be provided or both omitted");
		}
		if (targetWeight != null && targetWeight.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("targetWeight must be >= 0");
		}
		if (targetDurationSeconds != null && targetDurationSeconds < 0) {
			throw new IllegalArgumentException("targetDurationSeconds must be >= 0");
		}
		if ((targetDistance == null) != (distanceUnit == null)) {
			throw new IllegalArgumentException("targetDistance and distanceUnit must both be provided or both omitted");
		}
		if (targetDistance != null && targetDistance.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("targetDistance must be >= 0");
		}
		if (targetRestSeconds != null && targetRestSeconds < 0) {
			throw new IllegalArgumentException("targetRestSeconds must be >= 0");
		}
		if (targetRpe != null && (targetRpe < 0 || targetRpe > 10)) {
			throw new IllegalArgumentException("targetRpe must be between 0 and 10");
		}
		String normalizedTempo = normalizeOptionalText(tempo, MAX_TEMPO_LENGTH, "tempo");
		String normalizedNotes = normalizeOptionalText(coachingNotes, MAX_COACHING_NOTES_LENGTH, "coachingNotes");
		return new Prescription(
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				targetDurationSeconds,
				targetDistance,
				distanceUnit,
				targetRestSeconds,
				targetRpe,
				normalizedTempo,
				normalizedNotes);
	}

	private static String normalizeOptionalText(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	private record Prescription(
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes) {
	}

	public WorkoutExerciseId id() {
		return id;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int displayOrder() {
		return displayOrder;
	}

	public String exerciseName() {
		return exerciseName;
	}

	public String normalizedExerciseName() {
		return normalizedExerciseName;
	}

	public ExerciseCategory category() {
		return category;
	}

	public ExerciseType type() {
		return type;
	}

	public Integer sets() {
		return sets;
	}

	public Integer minimumReps() {
		return minimumReps;
	}

	public Integer maximumReps() {
		return maximumReps;
	}

	public BigDecimal targetWeight() {
		return targetWeight;
	}

	public WeightUnit weightUnit() {
		return weightUnit;
	}

	public Integer targetDurationSeconds() {
		return targetDurationSeconds;
	}

	public BigDecimal targetDistance() {
		return targetDistance;
	}

	public DistanceUnit distanceUnit() {
		return distanceUnit;
	}

	public Integer targetRestSeconds() {
		return targetRestSeconds;
	}

	public Integer targetRpe() {
		return targetRpe;
	}

	public String tempo() {
		return tempo;
	}

	public String coachingNotes() {
		return coachingNotes;
	}

	public WorkoutExerciseStatus status() {
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
