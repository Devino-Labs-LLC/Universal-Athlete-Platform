package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;

@Entity
@Table(name = "workout_adaptation_proposals")
class WorkoutAdaptationProposalJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_occurrence_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID workoutOccurrenceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "environment_context_source", nullable = false, length = 40)
	private FeasibilityEnvironmentContextSource environmentContextSource;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_environment_id", columnDefinition = "BINARY(16)")
	private UUID trainingEnvironmentId;

	@Column(name = "environment_name_snapshot", length = 100)
	private String environmentNameSnapshot;

	@Column(name = "occurrence_version_at_generation", nullable = false)
	private long occurrenceVersionAtGeneration;

	@Column(name = "occurrence_updated_at_at_generation")
	private Instant occurrenceUpdatedAtAtGeneration;

	@Column(name = "feasibility_fingerprint", nullable = false, length = 64)
	private String feasibilityFingerprint;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private WorkoutAdaptationProposalStatus status;

	@Column(name = "total_executions", nullable = false)
	private int totalExecutions;

	@Column(name = "already_feasible_executions", nullable = false)
	private int alreadyFeasibleExecutions;

	@Column(name = "proposed_substitutions", nullable = false)
	private int proposedSubstitutions;

	@Column(name = "unresolved_executions", nullable = false)
	private int unresolvedExecutions;

	@Column(name = "excluded_executions", nullable = false)
	private int excludedExecutions;

	@Column(name = "expected_feasible_executions", nullable = false)
	private int expectedFeasibleExecutions;

	@Column(name = "expected_feasibility_percentage", precision = 7, scale = 2)
	private BigDecimal expectedFeasibilityPercentage;

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "applied_at")
	private Instant appliedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "workout_adaptation_proposal_equipment_snapshot",
			joinColumns = @JoinColumn(name = "proposal_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> availableEquipmentSnapshot = new LinkedHashSet<>();

	@OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("executionOrder ASC, id ASC")
	private List<WorkoutAdaptationProposalItemJpaEntity> items = new ArrayList<>();

	protected WorkoutAdaptationProposalJpaEntity() {
	}

	UUID getId() {
		return id;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getTrainingPlanId() {
		return trainingPlanId;
	}

	UUID getWorkoutDayId() {
		return workoutDayId;
	}

	UUID getWorkoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	FeasibilityEnvironmentContextSource getEnvironmentContextSource() {
		return environmentContextSource;
	}

	UUID getTrainingEnvironmentId() {
		return trainingEnvironmentId;
	}

	String getEnvironmentNameSnapshot() {
		return environmentNameSnapshot;
	}

	long getOccurrenceVersionAtGeneration() {
		return occurrenceVersionAtGeneration;
	}

	Instant getOccurrenceUpdatedAtAtGeneration() {
		return occurrenceUpdatedAtAtGeneration;
	}

	String getFeasibilityFingerprint() {
		return feasibilityFingerprint;
	}

	WorkoutAdaptationProposalStatus getStatus() {
		return status;
	}

	int getTotalExecutions() {
		return totalExecutions;
	}

	int getAlreadyFeasibleExecutions() {
		return alreadyFeasibleExecutions;
	}

	int getProposedSubstitutions() {
		return proposedSubstitutions;
	}

	int getUnresolvedExecutions() {
		return unresolvedExecutions;
	}

	int getExcludedExecutions() {
		return excludedExecutions;
	}

	int getExpectedFeasibleExecutions() {
		return expectedFeasibleExecutions;
	}

	BigDecimal getExpectedFeasibilityPercentage() {
		return expectedFeasibilityPercentage;
	}

	Instant getGeneratedAt() {
		return generatedAt;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	Instant getAppliedAt() {
		return appliedAt;
	}

	Instant getCancelledAt() {
		return cancelledAt;
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

	Set<EquipmentType> getAvailableEquipmentSnapshot() {
		return availableEquipmentSnapshot;
	}

	List<WorkoutAdaptationProposalItemJpaEntity> getItems() {
		return items;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	void setTrainingPlanId(UUID trainingPlanId) {
		this.trainingPlanId = trainingPlanId;
	}

	void setWorkoutDayId(UUID workoutDayId) {
		this.workoutDayId = workoutDayId;
	}

	void setWorkoutOccurrenceId(UUID workoutOccurrenceId) {
		this.workoutOccurrenceId = workoutOccurrenceId;
	}

	void setEnvironmentContextSource(FeasibilityEnvironmentContextSource environmentContextSource) {
		this.environmentContextSource = environmentContextSource;
	}

	void setTrainingEnvironmentId(UUID trainingEnvironmentId) {
		this.trainingEnvironmentId = trainingEnvironmentId;
	}

	void setEnvironmentNameSnapshot(String environmentNameSnapshot) {
		this.environmentNameSnapshot = environmentNameSnapshot;
	}

	void setOccurrenceVersionAtGeneration(long occurrenceVersionAtGeneration) {
		this.occurrenceVersionAtGeneration = occurrenceVersionAtGeneration;
	}

	void setOccurrenceUpdatedAtAtGeneration(Instant occurrenceUpdatedAtAtGeneration) {
		this.occurrenceUpdatedAtAtGeneration = occurrenceUpdatedAtAtGeneration;
	}

	void setFeasibilityFingerprint(String feasibilityFingerprint) {
		this.feasibilityFingerprint = feasibilityFingerprint;
	}

	void setStatus(WorkoutAdaptationProposalStatus status) {
		this.status = status;
	}

	void setTotalExecutions(int totalExecutions) {
		this.totalExecutions = totalExecutions;
	}

	void setAlreadyFeasibleExecutions(int alreadyFeasibleExecutions) {
		this.alreadyFeasibleExecutions = alreadyFeasibleExecutions;
	}

	void setProposedSubstitutions(int proposedSubstitutions) {
		this.proposedSubstitutions = proposedSubstitutions;
	}

	void setUnresolvedExecutions(int unresolvedExecutions) {
		this.unresolvedExecutions = unresolvedExecutions;
	}

	void setExcludedExecutions(int excludedExecutions) {
		this.excludedExecutions = excludedExecutions;
	}

	void setExpectedFeasibleExecutions(int expectedFeasibleExecutions) {
		this.expectedFeasibleExecutions = expectedFeasibleExecutions;
	}

	void setExpectedFeasibilityPercentage(BigDecimal expectedFeasibilityPercentage) {
		this.expectedFeasibilityPercentage = expectedFeasibilityPercentage;
	}

	void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	void setAppliedAt(Instant appliedAt) {
		this.appliedAt = appliedAt;
	}

	void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
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

	void setAvailableEquipmentSnapshot(Set<EquipmentType> availableEquipmentSnapshot) {
		this.availableEquipmentSnapshot = availableEquipmentSnapshot == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(availableEquipmentSnapshot);
	}

	void setItems(List<WorkoutAdaptationProposalItemJpaEntity> items) {
		this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
	}

}
