package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.RecoveryCheckInSource;

@Entity
@Table(name = "daily_recovery_check_ins")
@NamedEntityGraph(
		name = DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT,
		attributeNodes = @NamedAttributeNode("discomfort"))
class DailyRecoveryCheckInJpaEntity implements Persistable<UUID> {

	static final String WITH_DISCOMFORT = "DailyRecoveryCheckInJpaEntity.withDiscomfort";

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "check_in_date", nullable = false, updatable = false)
	private LocalDate checkInDate;

	@Column(name = "sleep_duration_minutes")
	private Integer sleepDurationMinutes;

	@Column(name = "sleep_quality")
	private Integer sleepQuality;

	@Column(name = "fatigue", nullable = false)
	private int fatigue;

	@Column(name = "muscle_soreness", nullable = false)
	private int muscleSoreness;

	@Column(name = "stress", nullable = false)
	private int stress;

	@Column(name = "mood", nullable = false)
	private int mood;

	@Column(name = "motivation", nullable = false)
	private int motivation;

	@Enumerated(EnumType.STRING)
	@Column(name = "completeness", nullable = false, length = 16)
	private RecoveryCheckInCompleteness completeness;

	@Column(name = "notes", length = 2000)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, updatable = false, length = 32)
	private RecoveryCheckInSource source;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@OneToMany(mappedBy = "checkIn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<DailyRecoveryCheckInDiscomfortJpaEntity> discomfort = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected DailyRecoveryCheckInJpaEntity() {
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

	LocalDate getCheckInDate() {
		return checkInDate;
	}

	Integer getSleepDurationMinutes() {
		return sleepDurationMinutes;
	}

	Integer getSleepQuality() {
		return sleepQuality;
	}

	int getFatigue() {
		return fatigue;
	}

	int getMuscleSoreness() {
		return muscleSoreness;
	}

	int getStress() {
		return stress;
	}

	int getMood() {
		return mood;
	}

	int getMotivation() {
		return motivation;
	}

	RecoveryCheckInCompleteness getCompleteness() {
		return completeness;
	}

	String getNotes() {
		return notes;
	}

	RecoveryCheckInSource getSource() {
		return source;
	}

	Instant getSubmittedAt() {
		return submittedAt;
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

	List<DailyRecoveryCheckInDiscomfortJpaEntity> getDiscomfort() {
		return discomfort;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	void setCheckInDate(LocalDate checkInDate) {
		this.checkInDate = checkInDate;
	}

	void setSleepDurationMinutes(Integer sleepDurationMinutes) {
		this.sleepDurationMinutes = sleepDurationMinutes;
	}

	void setSleepQuality(Integer sleepQuality) {
		this.sleepQuality = sleepQuality;
	}

	void setFatigue(int fatigue) {
		this.fatigue = fatigue;
	}

	void setMuscleSoreness(int muscleSoreness) {
		this.muscleSoreness = muscleSoreness;
	}

	void setStress(int stress) {
		this.stress = stress;
	}

	void setMood(int mood) {
		this.mood = mood;
	}

	void setMotivation(int motivation) {
		this.motivation = motivation;
	}

	void setCompleteness(RecoveryCheckInCompleteness completeness) {
		this.completeness = completeness;
	}

	void setNotes(String notes) {
		this.notes = notes;
	}

	void setSource(RecoveryCheckInSource source) {
		this.source = source;
	}

	void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}

	void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	void setVersion(long version) {
		this.version = version;
	}

	void setDiscomfort(List<DailyRecoveryCheckInDiscomfortJpaEntity> discomfort) {
		this.discomfort = discomfort;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
