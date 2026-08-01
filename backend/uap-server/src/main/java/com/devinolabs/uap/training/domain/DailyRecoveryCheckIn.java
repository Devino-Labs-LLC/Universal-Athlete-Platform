package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Athlete-reported daily recovery and wellness observations for one calendar date.
 *
 * <p>One current check-in per athlete and date. Updates preserve aggregate identity and append
 * immutable revisions. Training load is never persisted here — it is read-only context from Phase 7N.
 */
public final class DailyRecoveryCheckIn {

	public static final int MAX_NOTES_LENGTH = 2000;
	public static final int MIN_SLEEP_DURATION = 0;
	public static final int MAX_SLEEP_DURATION = 1440;

	private final DailyRecoveryCheckInId id;
	private final AthleteId athleteId;
	private final LocalDate checkInDate;
	private Integer sleepDurationMinutes;
	private SleepQualityRating sleepQuality;
	private FatigueRating fatigue;
	private MuscleSorenessRating muscleSoreness;
	private StressRating stress;
	private MoodRating mood;
	private TrainingMotivationRating motivation;
	private RecoveryCheckInCompleteness completeness;
	private List<BodyAreaDiscomfortObservation> discomfortAreas;
	private String notes;
	private final RecoveryCheckInSource source;
	private Instant submittedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private DailyRecoveryCheckIn(
			DailyRecoveryCheckInId id,
			AthleteId athleteId,
			LocalDate checkInDate,
			Integer sleepDurationMinutes,
			SleepQualityRating sleepQuality,
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation,
			RecoveryCheckInCompleteness completeness,
			List<BodyAreaDiscomfortObservation> discomfortAreas,
			String notes,
			RecoveryCheckInSource source,
			Instant submittedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.checkInDate = Objects.requireNonNull(checkInDate, "checkInDate must not be null");
		this.sleepDurationMinutes = sleepDurationMinutes;
		this.sleepQuality = sleepQuality;
		this.fatigue = Objects.requireNonNull(fatigue, "fatigue must not be null");
		this.muscleSoreness = Objects.requireNonNull(muscleSoreness, "muscleSoreness must not be null");
		this.stress = Objects.requireNonNull(stress, "stress must not be null");
		this.mood = Objects.requireNonNull(mood, "mood must not be null");
		this.motivation = Objects.requireNonNull(motivation, "motivation must not be null");
		this.completeness = Objects.requireNonNull(completeness, "completeness must not be null");
		this.discomfortAreas = List.copyOf(discomfortAreas);
		this.notes = notes;
		this.source = Objects.requireNonNull(source, "source must not be null");
		this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		this.version = version;
		validateSleepDuration(sleepDurationMinutes);
	}

	public static DailyRecoveryCheckIn create(
			AthleteId athleteId,
			LocalDate checkInDate,
			Integer sleepDurationMinutes,
			SleepQualityRating sleepQuality,
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation,
			List<BodyAreaDiscomfortObservation> discomfortAreas,
			String notes,
			RecoveryCheckInSource source,
			Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		RecoveryCheckInDateValidator.validate(checkInDate, clock);
		RecoveryCheckInCompleteness completeness = RecoveryCheckInCompletenessCalculator.calculate(
				fatigue, muscleSoreness, stress, mood, motivation);
		Instant now = Instant.now(clock);
		return new DailyRecoveryCheckIn(
				DailyRecoveryCheckInId.generate(),
				athleteId,
				checkInDate,
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				completeness,
				discomfortAreas == null ? List.of() : discomfortAreas,
				normalizeNotes(notes),
				source,
				now,
				now,
				now,
				0L);
	}

	public static DailyRecoveryCheckIn rehydrate(
			DailyRecoveryCheckInId id,
			AthleteId athleteId,
			LocalDate checkInDate,
			Integer sleepDurationMinutes,
			SleepQualityRating sleepQuality,
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation,
			RecoveryCheckInCompleteness completeness,
			List<BodyAreaDiscomfortObservation> discomfortAreas,
			String notes,
			RecoveryCheckInSource source,
			Instant submittedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new DailyRecoveryCheckIn(
				id,
				athleteId,
				checkInDate,
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				completeness,
				discomfortAreas == null ? List.of() : discomfortAreas,
				notes,
				source,
				submittedAt,
				createdAt,
				updatedAt,
				version);
	}

	/**
	 * Applies a material update and returns a revision, or empty when the resolved snapshot equals
	 * the current state (no-op — no revision row).
	 */
	public Optional<DailyRecoveryCheckInRevision> update(Snapshot updated, int nextRevisionNumber, Clock clock) {
		Objects.requireNonNull(updated, "updated must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		Snapshot prior = snapshot();
		if (prior.equals(updated)) {
			return Optional.empty();
		}
		Instant changedAt = Instant.now(clock);
		DailyRecoveryCheckInRevision revision = DailyRecoveryCheckInRevision.create(
				this, prior, updated, nextRevisionNumber, changedAt);
		applySnapshot(updated);
		this.submittedAt = changedAt;
		this.updatedAt = changedAt;
		return Optional.of(revision);
	}

	private void applySnapshot(Snapshot updated) {
		validateSleepDuration(updated.sleepDurationMinutes());
		this.sleepDurationMinutes = updated.sleepDurationMinutes();
		this.sleepQuality = updated.sleepQuality();
		this.fatigue = updated.fatigue();
		this.muscleSoreness = updated.muscleSoreness();
		this.stress = updated.stress();
		this.mood = updated.mood();
		this.motivation = updated.motivation();
		this.completeness = updated.completeness();
		this.discomfortAreas = updated.discomfortAreas();
		this.notes = updated.notes();
	}

	public Snapshot snapshot() {
		return new Snapshot(
				sleepDurationMinutes,
				sleepQuality,
				fatigue,
				muscleSoreness,
				stress,
				mood,
				motivation,
				completeness,
				discomfortAreas,
				notes);
	}

	public static void validateSleepDuration(Integer sleepDurationMinutes) {
		if (sleepDurationMinutes == null) {
			return;
		}
		if (sleepDurationMinutes < MIN_SLEEP_DURATION || sleepDurationMinutes > MAX_SLEEP_DURATION) {
			throw new InvalidSleepDurationException(
					"sleepDurationMinutes must be between " + MIN_SLEEP_DURATION + " and "
							+ MAX_SLEEP_DURATION + " inclusive");
		}
	}

	public static String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String trimmed = notes.trim();
		if (trimmed.length() > MAX_NOTES_LENGTH) {
			throw new InvalidRecoveryCheckInNotesException(
					"notes must not exceed " + MAX_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	public DailyRecoveryCheckInId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public LocalDate checkInDate() {
		return checkInDate;
	}

	public Integer sleepDurationMinutes() {
		return sleepDurationMinutes;
	}

	public SleepQualityRating sleepQuality() {
		return sleepQuality;
	}

	public FatigueRating fatigue() {
		return fatigue;
	}

	public MuscleSorenessRating muscleSoreness() {
		return muscleSoreness;
	}

	public StressRating stress() {
		return stress;
	}

	public MoodRating mood() {
		return mood;
	}

	public TrainingMotivationRating motivation() {
		return motivation;
	}

	public RecoveryCheckInCompleteness completeness() {
		return completeness;
	}

	public List<BodyAreaDiscomfortObservation> discomfortAreas() {
		return discomfortAreas;
	}

	public String notes() {
		return notes;
	}

	public RecoveryCheckInSource source() {
		return source;
	}

	public Instant submittedAt() {
		return submittedAt;
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

	public record Snapshot(
			Integer sleepDurationMinutes,
			SleepQualityRating sleepQuality,
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation,
			RecoveryCheckInCompleteness completeness,
			List<BodyAreaDiscomfortObservation> discomfortAreas,
			String notes) {

		public Snapshot {
			discomfortAreas = discomfortAreas == null ? List.of() : List.copyOf(discomfortAreas);
		}

		public static Snapshot fromCurrent(DailyRecoveryCheckIn checkIn) {
			return checkIn.snapshot();
		}

		public static Snapshot merge(DailyRecoveryCheckIn current, UpdateFields fields) {
			Integer sleepDuration = fields.sleepDurationMinutesPresent()
					? fields.sleepDurationMinutes()
					: current.sleepDurationMinutes();
			SleepQualityRating sleepQuality = fields.sleepQualityPresent()
					? fields.sleepQuality()
					: current.sleepQuality();
			FatigueRating fatigue = fields.fatiguePresent()
					? fields.fatigue()
					: current.fatigue();
			MuscleSorenessRating muscleSoreness = fields.muscleSorenessPresent()
					? fields.muscleSoreness()
					: current.muscleSoreness();
			StressRating stress = fields.stressPresent()
					? fields.stress()
					: current.stress();
			MoodRating mood = fields.moodPresent()
					? fields.mood()
					: current.mood();
			TrainingMotivationRating motivation = fields.motivationPresent()
					? fields.motivation()
					: current.motivation();
			List<BodyAreaDiscomfortObservation> discomfort = fields.discomfortAreasPresent()
					? fields.discomfortAreas()
					: current.discomfortAreas();
			String notes = fields.notesPresent()
					? normalizeNotes(fields.notes())
					: current.notes();
			RecoveryCheckInCompleteness completeness = RecoveryCheckInCompletenessCalculator.calculate(
					fatigue, muscleSoreness, stress, mood, motivation);
			return new Snapshot(
					sleepDuration,
					sleepQuality,
					fatigue,
					muscleSoreness,
					stress,
					mood,
					motivation,
					completeness,
					discomfort,
					notes);
		}
	}

	public record UpdateFields(
			Integer sleepDurationMinutes,
			boolean sleepDurationMinutesPresent,
			SleepQualityRating sleepQuality,
			boolean sleepQualityPresent,
			FatigueRating fatigue,
			boolean fatiguePresent,
			MuscleSorenessRating muscleSoreness,
			boolean muscleSorenessPresent,
			StressRating stress,
			boolean stressPresent,
			MoodRating mood,
			boolean moodPresent,
			TrainingMotivationRating motivation,
			boolean motivationPresent,
			List<BodyAreaDiscomfortObservation> discomfortAreas,
			boolean discomfortAreasPresent,
			String notes,
			boolean notesPresent) {
	}

}
