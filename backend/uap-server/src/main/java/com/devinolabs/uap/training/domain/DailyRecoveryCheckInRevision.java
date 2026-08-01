package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class DailyRecoveryCheckInRevision {

	private final DailyRecoveryCheckInRevisionId id;
	private final DailyRecoveryCheckInId recoveryCheckInId;
	private final AthleteId athleteId;
	private final int revisionNumber;
	private final Integer priorSleepDurationMinutes;
	private final Integer newSleepDurationMinutes;
	private final SleepQualityRating priorSleepQuality;
	private final SleepQualityRating newSleepQuality;
	private final FatigueRating priorFatigue;
	private final FatigueRating newFatigue;
	private final MuscleSorenessRating priorMuscleSoreness;
	private final MuscleSorenessRating newMuscleSoreness;
	private final StressRating priorStress;
	private final StressRating newStress;
	private final MoodRating priorMood;
	private final MoodRating newMood;
	private final TrainingMotivationRating priorMotivation;
	private final TrainingMotivationRating newMotivation;
	private final RecoveryCheckInCompleteness priorCompleteness;
	private final RecoveryCheckInCompleteness newCompleteness;
	private final String priorNotes;
	private final String newNotes;
	private final List<BodyAreaDiscomfortObservation> priorDiscomfort;
	private final List<BodyAreaDiscomfortObservation> newDiscomfort;
	private final Instant changedAt;
	private final Instant createdAt;

	private DailyRecoveryCheckInRevision(
			DailyRecoveryCheckInRevisionId id,
			DailyRecoveryCheckInId recoveryCheckInId,
			AthleteId athleteId,
			int revisionNumber,
			Integer priorSleepDurationMinutes,
			Integer newSleepDurationMinutes,
			SleepQualityRating priorSleepQuality,
			SleepQualityRating newSleepQuality,
			FatigueRating priorFatigue,
			FatigueRating newFatigue,
			MuscleSorenessRating priorMuscleSoreness,
			MuscleSorenessRating newMuscleSoreness,
			StressRating priorStress,
			StressRating newStress,
			MoodRating priorMood,
			MoodRating newMood,
			TrainingMotivationRating priorMotivation,
			TrainingMotivationRating newMotivation,
			RecoveryCheckInCompleteness priorCompleteness,
			RecoveryCheckInCompleteness newCompleteness,
			String priorNotes,
			String newNotes,
			List<BodyAreaDiscomfortObservation> priorDiscomfort,
			List<BodyAreaDiscomfortObservation> newDiscomfort,
			Instant changedAt,
			Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.recoveryCheckInId = Objects.requireNonNull(recoveryCheckInId, "recoveryCheckInId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		if (revisionNumber < 1) {
			throw new IllegalArgumentException("revisionNumber must be >= 1");
		}
		this.revisionNumber = revisionNumber;
		this.priorSleepDurationMinutes = priorSleepDurationMinutes;
		this.newSleepDurationMinutes = newSleepDurationMinutes;
		this.priorSleepQuality = priorSleepQuality;
		this.newSleepQuality = newSleepQuality;
		this.priorFatigue = Objects.requireNonNull(priorFatigue, "priorFatigue must not be null");
		this.newFatigue = Objects.requireNonNull(newFatigue, "newFatigue must not be null");
		this.priorMuscleSoreness = Objects.requireNonNull(priorMuscleSoreness, "priorMuscleSoreness must not be null");
		this.newMuscleSoreness = Objects.requireNonNull(newMuscleSoreness, "newMuscleSoreness must not be null");
		this.priorStress = Objects.requireNonNull(priorStress, "priorStress must not be null");
		this.newStress = Objects.requireNonNull(newStress, "newStress must not be null");
		this.priorMood = Objects.requireNonNull(priorMood, "priorMood must not be null");
		this.newMood = Objects.requireNonNull(newMood, "newMood must not be null");
		this.priorMotivation = Objects.requireNonNull(priorMotivation, "priorMotivation must not be null");
		this.newMotivation = Objects.requireNonNull(newMotivation, "newMotivation must not be null");
		this.priorCompleteness = Objects.requireNonNull(priorCompleteness, "priorCompleteness must not be null");
		this.newCompleteness = Objects.requireNonNull(newCompleteness, "newCompleteness must not be null");
		this.priorNotes = priorNotes;
		this.newNotes = newNotes;
		this.priorDiscomfort = List.copyOf(priorDiscomfort);
		this.newDiscomfort = List.copyOf(newDiscomfort);
		this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	static DailyRecoveryCheckInRevision create(
			DailyRecoveryCheckIn checkIn,
			DailyRecoveryCheckIn.Snapshot prior,
			DailyRecoveryCheckIn.Snapshot updated,
			int revisionNumber,
			Instant changedAt) {
		return new DailyRecoveryCheckInRevision(
				DailyRecoveryCheckInRevisionId.generate(),
				checkIn.id(),
				checkIn.athleteId(),
				revisionNumber,
				prior.sleepDurationMinutes(),
				updated.sleepDurationMinutes(),
				prior.sleepQuality(),
				updated.sleepQuality(),
				prior.fatigue(),
				updated.fatigue(),
				prior.muscleSoreness(),
				updated.muscleSoreness(),
				prior.stress(),
				updated.stress(),
				prior.mood(),
				updated.mood(),
				prior.motivation(),
				updated.motivation(),
				prior.completeness(),
				updated.completeness(),
				prior.notes(),
				updated.notes(),
				prior.discomfortAreas(),
				updated.discomfortAreas(),
				changedAt,
				changedAt);
	}

	public static DailyRecoveryCheckInRevision rehydrate(
			DailyRecoveryCheckInRevisionId id,
			DailyRecoveryCheckInId recoveryCheckInId,
			AthleteId athleteId,
			int revisionNumber,
			Integer priorSleepDurationMinutes,
			Integer newSleepDurationMinutes,
			SleepQualityRating priorSleepQuality,
			SleepQualityRating newSleepQuality,
			FatigueRating priorFatigue,
			FatigueRating newFatigue,
			MuscleSorenessRating priorMuscleSoreness,
			MuscleSorenessRating newMuscleSoreness,
			StressRating priorStress,
			StressRating newStress,
			MoodRating priorMood,
			MoodRating newMood,
			TrainingMotivationRating priorMotivation,
			TrainingMotivationRating newMotivation,
			RecoveryCheckInCompleteness priorCompleteness,
			RecoveryCheckInCompleteness newCompleteness,
			String priorNotes,
			String newNotes,
			List<BodyAreaDiscomfortObservation> priorDiscomfort,
			List<BodyAreaDiscomfortObservation> newDiscomfort,
			Instant changedAt,
			Instant createdAt) {
		return new DailyRecoveryCheckInRevision(
				id,
				recoveryCheckInId,
				athleteId,
				revisionNumber,
				priorSleepDurationMinutes,
				newSleepDurationMinutes,
				priorSleepQuality,
				newSleepQuality,
				priorFatigue,
				newFatigue,
				priorMuscleSoreness,
				newMuscleSoreness,
				priorStress,
				newStress,
				priorMood,
				newMood,
				priorMotivation,
				newMotivation,
				priorCompleteness,
				newCompleteness,
				priorNotes,
				newNotes,
				priorDiscomfort,
				newDiscomfort,
				changedAt,
				createdAt);
	}

	public DailyRecoveryCheckInRevisionId id() {
		return id;
	}

	public DailyRecoveryCheckInId recoveryCheckInId() {
		return recoveryCheckInId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public int revisionNumber() {
		return revisionNumber;
	}

	public Integer priorSleepDurationMinutes() {
		return priorSleepDurationMinutes;
	}

	public Integer newSleepDurationMinutes() {
		return newSleepDurationMinutes;
	}

	public SleepQualityRating priorSleepQuality() {
		return priorSleepQuality;
	}

	public SleepQualityRating newSleepQuality() {
		return newSleepQuality;
	}

	public FatigueRating priorFatigue() {
		return priorFatigue;
	}

	public FatigueRating newFatigue() {
		return newFatigue;
	}

	public MuscleSorenessRating priorMuscleSoreness() {
		return priorMuscleSoreness;
	}

	public MuscleSorenessRating newMuscleSoreness() {
		return newMuscleSoreness;
	}

	public StressRating priorStress() {
		return priorStress;
	}

	public StressRating newStress() {
		return newStress;
	}

	public MoodRating priorMood() {
		return priorMood;
	}

	public MoodRating newMood() {
		return newMood;
	}

	public TrainingMotivationRating priorMotivation() {
		return priorMotivation;
	}

	public TrainingMotivationRating newMotivation() {
		return newMotivation;
	}

	public RecoveryCheckInCompleteness priorCompleteness() {
		return priorCompleteness;
	}

	public RecoveryCheckInCompleteness newCompleteness() {
		return newCompleteness;
	}

	public String priorNotes() {
		return priorNotes;
	}

	public String newNotes() {
		return newNotes;
	}

	public List<BodyAreaDiscomfortObservation> priorDiscomfort() {
		return priorDiscomfort;
	}

	public List<BodyAreaDiscomfortObservation> newDiscomfort() {
		return newDiscomfort;
	}

	public Instant changedAt() {
		return changedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
