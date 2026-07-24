package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.SportType;

@Entity
@Table(name = "athlete_sports")
class AthleteSportJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "sport_type", nullable = false, length = 40, updatable = false)
	private SportType sportType;

	@Column(name = "custom_sport_name", length = 100)
	private String customSportName;

	@Column(name = "custom_sport_name_normalized", length = 100)
	private String customSportNameNormalized;

	@Column(name = "is_primary", nullable = false)
	private boolean primarySport;

	@Enumerated(EnumType.STRING)
	@Column(name = "participation_level", nullable = false, length = 40)
	private ParticipationLevel participationLevel;

	@Column(name = "preferred_position", length = 100)
	private String preferredPosition;

	@Column(name = "years_experience", nullable = false)
	private int yearsExperience;

	@Enumerated(EnumType.STRING)
	@Column(name = "season_status", nullable = false, length = 30)
	private SeasonStatus seasonStatus;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AthleteSportJpaEntity() {
	}

	AthleteSportJpaEntity(
			UUID id,
			UUID athleteId,
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
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.sportType = sportType;
		this.customSportName = customSportName;
		this.customSportNameNormalized = customSportNameNormalized;
		this.primarySport = primarySport;
		this.participationLevel = participationLevel;
		this.preferredPosition = preferredPosition;
		this.yearsExperience = yearsExperience;
		this.seasonStatus = seasonStatus;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	SportType getSportType() {
		return sportType;
	}

	String getCustomSportName() {
		return customSportName;
	}

	String getCustomSportNameNormalized() {
		return customSportNameNormalized;
	}

	boolean isPrimarySport() {
		return primarySport;
	}

	ParticipationLevel getParticipationLevel() {
		return participationLevel;
	}

	String getPreferredPosition() {
		return preferredPosition;
	}

	int getYearsExperience() {
		return yearsExperience;
	}

	SeasonStatus getSeasonStatus() {
		return seasonStatus;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

}
