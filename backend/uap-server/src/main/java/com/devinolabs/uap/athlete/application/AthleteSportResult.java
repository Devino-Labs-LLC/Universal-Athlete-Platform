package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.Objects;

import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.SportType;

public final class AthleteSportResult {

	private final AthleteSportId id;
	private final SportType sportType;
	private final String customSportName;
	private final boolean primarySport;
	private final ParticipationLevel participationLevel;
	private final String preferredPosition;
	private final int yearsExperience;
	private final SeasonStatus seasonStatus;
	private final Instant createdAt;
	private final Instant updatedAt;

	public AthleteSportResult(
			AthleteSportId id,
			SportType sportType,
			String customSportName,
			boolean primarySport,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus,
			Instant createdAt,
			Instant updatedAt) {
		this.id = Objects.requireNonNull(id);
		this.sportType = Objects.requireNonNull(sportType);
		this.customSportName = customSportName;
		this.primarySport = primarySport;
		this.participationLevel = Objects.requireNonNull(participationLevel);
		this.preferredPosition = preferredPosition;
		this.yearsExperience = yearsExperience;
		this.seasonStatus = Objects.requireNonNull(seasonStatus);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = Objects.requireNonNull(updatedAt);
	}

	public AthleteSportId id() {
		return id;
	}

	public SportType sportType() {
		return sportType;
	}

	public String customSportName() {
		return customSportName;
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

}
