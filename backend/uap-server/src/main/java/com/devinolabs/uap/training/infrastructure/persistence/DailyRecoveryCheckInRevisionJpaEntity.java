package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

@Entity
@Table(name = "daily_recovery_check_in_revisions")
@NamedEntityGraph(
		name = DailyRecoveryCheckInRevisionJpaEntity.WITH_DISCOMFORT,
		attributeNodes = @NamedAttributeNode("discomfort"))
class DailyRecoveryCheckInRevisionJpaEntity implements Persistable<UUID> {

	static final String WITH_DISCOMFORT = "DailyRecoveryCheckInRevisionJpaEntity.withDiscomfort";

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "recovery_check_in_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID recoveryCheckInId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "revision_number", nullable = false, updatable = false)
	private int revisionNumber;

	@Column(name = "prior_sleep_duration_minutes", updatable = false)
	private Integer priorSleepDurationMinutes;

	@Column(name = "new_sleep_duration_minutes", updatable = false)
	private Integer newSleepDurationMinutes;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_sleep_quality", updatable = false)
	private Integer priorSleepQuality;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_sleep_quality", updatable = false)
	private Integer newSleepQuality;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_fatigue", nullable = false, updatable = false)
	private int priorFatigue;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_fatigue", nullable = false, updatable = false)
	private int newFatigue;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_muscle_soreness", nullable = false, updatable = false)
	private int priorMuscleSoreness;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_muscle_soreness", nullable = false, updatable = false)
	private int newMuscleSoreness;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_stress", nullable = false, updatable = false)
	private int priorStress;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_stress", nullable = false, updatable = false)
	private int newStress;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_mood", nullable = false, updatable = false)
	private int priorMood;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_mood", nullable = false, updatable = false)
	private int newMood;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "prior_motivation", nullable = false, updatable = false)
	private int priorMotivation;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "new_motivation", nullable = false, updatable = false)
	private int newMotivation;

	@Enumerated(EnumType.STRING)
	@Column(name = "prior_completeness", nullable = false, updatable = false, length = 16)
	private RecoveryCheckInCompleteness priorCompleteness;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_completeness", nullable = false, updatable = false, length = 16)
	private RecoveryCheckInCompleteness newCompleteness;

	@Column(name = "prior_notes", length = 2000, updatable = false)
	private String priorNotes;

	@Column(name = "new_notes", length = 2000, updatable = false)
	private String newNotes;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<DailyRecoveryCheckInRevisionDiscomfortJpaEntity> discomfort = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected DailyRecoveryCheckInRevisionJpaEntity() {
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

	UUID getRecoveryCheckInId() {
		return recoveryCheckInId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	int getRevisionNumber() {
		return revisionNumber;
	}

	Integer getPriorSleepDurationMinutes() {
		return priorSleepDurationMinutes;
	}

	Integer getNewSleepDurationMinutes() {
		return newSleepDurationMinutes;
	}

	Integer getPriorSleepQuality() {
		return priorSleepQuality;
	}

	Integer getNewSleepQuality() {
		return newSleepQuality;
	}

	int getPriorFatigue() {
		return priorFatigue;
	}

	int getNewFatigue() {
		return newFatigue;
	}

	int getPriorMuscleSoreness() {
		return priorMuscleSoreness;
	}

	int getNewMuscleSoreness() {
		return newMuscleSoreness;
	}

	int getPriorStress() {
		return priorStress;
	}

	int getNewStress() {
		return newStress;
	}

	int getPriorMood() {
		return priorMood;
	}

	int getNewMood() {
		return newMood;
	}

	int getPriorMotivation() {
		return priorMotivation;
	}

	int getNewMotivation() {
		return newMotivation;
	}

	RecoveryCheckInCompleteness getPriorCompleteness() {
		return priorCompleteness;
	}

	RecoveryCheckInCompleteness getNewCompleteness() {
		return newCompleteness;
	}

	String getPriorNotes() {
		return priorNotes;
	}

	String getNewNotes() {
		return newNotes;
	}

	Instant getChangedAt() {
		return changedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	List<DailyRecoveryCheckInRevisionDiscomfortJpaEntity> getDiscomfort() {
		return discomfort;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setRecoveryCheckInId(UUID recoveryCheckInId) {
		this.recoveryCheckInId = recoveryCheckInId;
	}

	void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	void setRevisionNumber(int revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	void setPriorSleepDurationMinutes(Integer priorSleepDurationMinutes) {
		this.priorSleepDurationMinutes = priorSleepDurationMinutes;
	}

	void setNewSleepDurationMinutes(Integer newSleepDurationMinutes) {
		this.newSleepDurationMinutes = newSleepDurationMinutes;
	}

	void setPriorSleepQuality(Integer priorSleepQuality) {
		this.priorSleepQuality = priorSleepQuality;
	}

	void setNewSleepQuality(Integer newSleepQuality) {
		this.newSleepQuality = newSleepQuality;
	}

	void setPriorFatigue(int priorFatigue) {
		this.priorFatigue = priorFatigue;
	}

	void setNewFatigue(int newFatigue) {
		this.newFatigue = newFatigue;
	}

	void setPriorMuscleSoreness(int priorMuscleSoreness) {
		this.priorMuscleSoreness = priorMuscleSoreness;
	}

	void setNewMuscleSoreness(int newMuscleSoreness) {
		this.newMuscleSoreness = newMuscleSoreness;
	}

	void setPriorStress(int priorStress) {
		this.priorStress = priorStress;
	}

	void setNewStress(int newStress) {
		this.newStress = newStress;
	}

	void setPriorMood(int priorMood) {
		this.priorMood = priorMood;
	}

	void setNewMood(int newMood) {
		this.newMood = newMood;
	}

	void setPriorMotivation(int priorMotivation) {
		this.priorMotivation = priorMotivation;
	}

	void setNewMotivation(int newMotivation) {
		this.newMotivation = newMotivation;
	}

	void setPriorCompleteness(RecoveryCheckInCompleteness priorCompleteness) {
		this.priorCompleteness = priorCompleteness;
	}

	void setNewCompleteness(RecoveryCheckInCompleteness newCompleteness) {
		this.newCompleteness = newCompleteness;
	}

	void setPriorNotes(String priorNotes) {
		this.priorNotes = priorNotes;
	}

	void setNewNotes(String newNotes) {
		this.newNotes = newNotes;
	}

	void setChangedAt(Instant changedAt) {
		this.changedAt = changedAt;
	}

	void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	void setDiscomfort(List<DailyRecoveryCheckInRevisionDiscomfortJpaEntity> discomfort) {
		this.discomfort = discomfort;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
