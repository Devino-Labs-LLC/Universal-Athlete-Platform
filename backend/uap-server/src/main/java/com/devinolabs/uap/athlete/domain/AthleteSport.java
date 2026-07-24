package com.devinolabs.uap.athlete.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class AthleteSport {

	private static final int MAX_CUSTOM_SPORT_NAME_LENGTH = 100;
	private static final int MAX_PREFERRED_POSITION_LENGTH = 100;
	private static final int MIN_YEARS_EXPERIENCE = 0;
	private static final int MAX_YEARS_EXPERIENCE = 80;

	private final AthleteSportId id;
	private final AthleteId athleteId;
	private final SportType sportType;
	private final String customSportName;
	private final String customSportNameNormalized;
	private boolean primarySport;
	private ParticipationLevel participationLevel;
	private String preferredPosition;
	private int yearsExperience;
	private SeasonStatus seasonStatus;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private AthleteSport(
			AthleteSportId id,
			AthleteId athleteId,
			SportType sportType,
			String customSportName,
			String customSportNameNormalized,
			boolean primarySport,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "AthleteSport id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "AthleteSport athleteId must not be null");
		this.sportType = Objects.requireNonNull(sportType, "AthleteSport sportType must not be null");
		validateCustomSportName(sportType, customSportName);
		this.customSportName = customSportName == null ? null : customSportName.trim();
		this.customSportNameNormalized = customSportNameNormalized;
		this.primarySport = primarySport;
		this.participationLevel = Objects.requireNonNull(participationLevel, "participationLevel must not be null");
		this.preferredPosition = normalizePreferredPosition(preferredPosition);
		this.yearsExperience = requireYearsExperience(yearsExperience);
		this.seasonStatus = Objects.requireNonNull(seasonStatus, "seasonStatus must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static AthleteSport register(
			AthleteSportId id,
			AthleteId athleteId,
			SportType sportType,
			String customSportName,
			boolean primarySport,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus) {
		return register(
				id,
				athleteId,
				sportType,
				customSportName,
				primarySport,
				participationLevel,
				preferredPosition,
				yearsExperience,
				seasonStatus,
				Clock.systemUTC());
	}

	public static AthleteSport register(
			AthleteSportId id,
			AthleteId athleteId,
			SportType sportType,
			String customSportName,
			boolean primarySport,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		String normalizedCustom = normalizeCustomSportName(sportType, customSportName);
		String displayCustom = sportType == SportType.OTHER ? customSportName.trim() : null;
		return new AthleteSport(
				id,
				athleteId,
				sportType,
				displayCustom,
				normalizedCustom,
				primarySport,
				participationLevel,
				preferredPosition,
				yearsExperience,
				seasonStatus,
				now,
				now,
				0L);
	}

	public static AthleteSport rehydrate(
			AthleteSportId id,
			AthleteId athleteId,
			SportType sportType,
			String customSportName,
			String customSportNameNormalized,
			boolean primarySport,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new AthleteSport(
				id,
				athleteId,
				sportType,
				customSportName,
				customSportNameNormalized,
				primarySport,
				participationLevel,
				preferredPosition,
				yearsExperience,
				seasonStatus,
				createdAt,
				updatedAt,
				version);
	}

	public void updateParticipation(
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.participationLevel = Objects.requireNonNull(participationLevel, "participationLevel must not be null");
		this.preferredPosition = normalizePreferredPosition(preferredPosition);
		this.yearsExperience = requireYearsExperience(yearsExperience);
		this.seasonStatus = Objects.requireNonNull(seasonStatus, "seasonStatus must not be null");
		touch(clock);
	}

	public void markPrimary(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (primarySport) {
			return;
		}
		this.primarySport = true;
		touch(clock);
	}

	public void unmarkPrimary(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!primarySport) {
			return;
		}
		this.primarySport = false;
		touch(clock);
	}

	public String displayName() {
		if (sportType == SportType.OTHER) {
			return customSportName;
		}
		return sportType.name();
	}

	public String sportIdentityKey() {
		if (sportType == SportType.OTHER) {
			return "OTHER:" + customSportNameNormalized;
		}
		return sportType.name();
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static void validateCustomSportName(SportType sportType, String customSportName) {
		if (sportType == SportType.OTHER) {
			if (customSportName == null || customSportName.isBlank()) {
				throw new IllegalArgumentException("customSportName is required when sportType is OTHER");
			}
			if (customSportName.trim().length() > MAX_CUSTOM_SPORT_NAME_LENGTH) {
				throw new IllegalArgumentException("customSportName must not exceed " + MAX_CUSTOM_SPORT_NAME_LENGTH + " characters");
			}
			return;
		}
		if (customSportName != null && !customSportName.isBlank()) {
			throw new IllegalArgumentException("customSportName must be absent unless sportType is OTHER");
		}
	}

	private static String normalizeCustomSportName(SportType sportType, String customSportName) {
		validateCustomSportName(sportType, customSportName);
		if (sportType != SportType.OTHER) {
			return null;
		}
		return customSportName.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizePreferredPosition(String preferredPosition) {
		if (preferredPosition == null || preferredPosition.isBlank()) {
			return null;
		}
		String normalized = preferredPosition.trim();
		if (normalized.length() > MAX_PREFERRED_POSITION_LENGTH) {
			throw new IllegalArgumentException(
					"preferredPosition must not exceed " + MAX_PREFERRED_POSITION_LENGTH + " characters");
		}
		return normalized;
	}

	private static int requireYearsExperience(int yearsExperience) {
		if (yearsExperience < MIN_YEARS_EXPERIENCE || yearsExperience > MAX_YEARS_EXPERIENCE) {
			throw new IllegalArgumentException(
					"yearsExperience must be between " + MIN_YEARS_EXPERIENCE + " and " + MAX_YEARS_EXPERIENCE);
		}
		return yearsExperience;
	}

	public AthleteSportId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public SportType sportType() {
		return sportType;
	}

	public String customSportName() {
		return customSportName;
	}

	public String customSportNameNormalized() {
		return customSportNameNormalized;
	}

	public boolean primarySport() {
		return primarySport;
	}

	public ParticipationLevel participationLevel() {
		return participationLevel;
	}

	public String preferredPosition() {
		return preferredPosition;
	}

	public int yearsExperience() {
		return yearsExperience;
	}

	public SeasonStatus seasonStatus() {
		return seasonStatus;
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
