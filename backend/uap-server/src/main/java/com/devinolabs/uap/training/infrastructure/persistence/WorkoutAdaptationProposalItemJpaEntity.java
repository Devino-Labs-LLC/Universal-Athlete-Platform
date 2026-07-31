package com.devinolabs.uap.training.infrastructure.persistence;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;

@Entity
@Table(name = "workout_adaptation_proposal_items")
class WorkoutAdaptationProposalItemJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposal_id", nullable = false, columnDefinition = "BINARY(16)")
	private WorkoutAdaptationProposalJpaEntity proposal;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_exercise_execution_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID workoutExerciseExecutionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_workout_exercise_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID sourceWorkoutExerciseId;

	@Column(name = "execution_order", nullable = false)
	private int executionOrder;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "prescribed_exercise_definition_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID prescribedExerciseDefinitionId;

	@Column(name = "prescribed_name_snapshot", nullable = false, length = 160)
	private String prescribedNameSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "current_performed_exercise_definition_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID currentPerformedExerciseDefinitionId;

	@Column(name = "current_performed_name_snapshot", nullable = false, length = 160)
	private String currentPerformedNameSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "exercise_performance_key_at_generation", nullable = false, columnDefinition = "BINARY(16)")
	private UUID exercisePerformanceKeyAtGeneration;

	@Column(name = "current_feasible", nullable = false)
	private boolean currentFeasible;

	@Column(name = "prescribed_feasible", nullable = false)
	private boolean prescribedFeasible;

	@Column(name = "performed_feasible", nullable = false)
	private boolean performedFeasible;

	@Enumerated(EnumType.STRING)
	@Column(name = "analysis_reason_code", nullable = false, length = 64)
	private FeasibilityReasonCode analysisReasonCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 32)
	private WorkoutAdaptationAction action;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "generated_target_exercise_definition_id", columnDefinition = "BINARY(16)")
	private UUID generatedTargetExerciseDefinitionId;

	@Column(name = "generated_target_name_snapshot", length = 160)
	private String generatedTargetNameSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "generated_relationship_id", columnDefinition = "BINARY(16)")
	private UUID generatedRelationshipId;

	@Enumerated(EnumType.STRING)
	@Column(name = "generated_relationship_type_snapshot", length = 40)
	private ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "generated_compatibility_snapshot", length = 32)
	private ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot;

	@Column(name = "generated_rationale_snapshot", length = 500)
	private String generatedRationaleSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "selected_target_exercise_definition_id", columnDefinition = "BINARY(16)")
	private UUID selectedTargetExerciseDefinitionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "selected_relationship_id", columnDefinition = "BINARY(16)")
	private UUID selectedRelationshipId;

	@Enumerated(EnumType.STRING)
	@Column(name = "athlete_decision", nullable = false, length = 32)
	private WorkoutAdaptationDecision athleteDecision;

	@Column(name = "athlete_notes", length = 2000)
	private String athleteNotes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "workout_adaptation_proposal_item_missing_equipment",
			joinColumns = @JoinColumn(name = "proposal_item_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> missingRequiredEquipment = new LinkedHashSet<>();

	@OneToMany(mappedBy = "proposalItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("rankPosition ASC, id ASC")
	private List<WorkoutAdaptationProposalItemAlternativeJpaEntity> alternatives = new ArrayList<>();

	protected WorkoutAdaptationProposalItemJpaEntity() {
	}

	UUID getId() {
		return id;
	}

	WorkoutAdaptationProposalJpaEntity getProposal() {
		return proposal;
	}

	UUID getWorkoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	UUID getSourceWorkoutExerciseId() {
		return sourceWorkoutExerciseId;
	}

	int getExecutionOrder() {
		return executionOrder;
	}

	UUID getPrescribedExerciseDefinitionId() {
		return prescribedExerciseDefinitionId;
	}

	String getPrescribedNameSnapshot() {
		return prescribedNameSnapshot;
	}

	UUID getCurrentPerformedExerciseDefinitionId() {
		return currentPerformedExerciseDefinitionId;
	}

	String getCurrentPerformedNameSnapshot() {
		return currentPerformedNameSnapshot;
	}

	UUID getExercisePerformanceKeyAtGeneration() {
		return exercisePerformanceKeyAtGeneration;
	}

	boolean isCurrentFeasible() {
		return currentFeasible;
	}

	boolean isPrescribedFeasible() {
		return prescribedFeasible;
	}

	boolean isPerformedFeasible() {
		return performedFeasible;
	}

	FeasibilityReasonCode getAnalysisReasonCode() {
		return analysisReasonCode;
	}

	WorkoutAdaptationAction getAction() {
		return action;
	}

	UUID getGeneratedTargetExerciseDefinitionId() {
		return generatedTargetExerciseDefinitionId;
	}

	String getGeneratedTargetNameSnapshot() {
		return generatedTargetNameSnapshot;
	}

	UUID getGeneratedRelationshipId() {
		return generatedRelationshipId;
	}

	ExerciseSubstitutionRelationshipType getGeneratedRelationshipTypeSnapshot() {
		return generatedRelationshipTypeSnapshot;
	}

	ExerciseSubstitutionCompatibility getGeneratedCompatibilitySnapshot() {
		return generatedCompatibilitySnapshot;
	}

	String getGeneratedRationaleSnapshot() {
		return generatedRationaleSnapshot;
	}

	UUID getSelectedTargetExerciseDefinitionId() {
		return selectedTargetExerciseDefinitionId;
	}

	UUID getSelectedRelationshipId() {
		return selectedRelationshipId;
	}

	WorkoutAdaptationDecision getAthleteDecision() {
		return athleteDecision;
	}

	String getAthleteNotes() {
		return athleteNotes;
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

	Set<EquipmentType> getMissingRequiredEquipment() {
		return missingRequiredEquipment;
	}

	List<WorkoutAdaptationProposalItemAlternativeJpaEntity> getAlternatives() {
		return alternatives;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setProposal(WorkoutAdaptationProposalJpaEntity proposal) {
		this.proposal = proposal;
	}

	void setWorkoutExerciseExecutionId(UUID workoutExerciseExecutionId) {
		this.workoutExerciseExecutionId = workoutExerciseExecutionId;
	}

	void setSourceWorkoutExerciseId(UUID sourceWorkoutExerciseId) {
		this.sourceWorkoutExerciseId = sourceWorkoutExerciseId;
	}

	void setExecutionOrder(int executionOrder) {
		this.executionOrder = executionOrder;
	}

	void setPrescribedExerciseDefinitionId(UUID prescribedExerciseDefinitionId) {
		this.prescribedExerciseDefinitionId = prescribedExerciseDefinitionId;
	}

	void setPrescribedNameSnapshot(String prescribedNameSnapshot) {
		this.prescribedNameSnapshot = prescribedNameSnapshot;
	}

	void setCurrentPerformedExerciseDefinitionId(UUID currentPerformedExerciseDefinitionId) {
		this.currentPerformedExerciseDefinitionId = currentPerformedExerciseDefinitionId;
	}

	void setCurrentPerformedNameSnapshot(String currentPerformedNameSnapshot) {
		this.currentPerformedNameSnapshot = currentPerformedNameSnapshot;
	}

	void setExercisePerformanceKeyAtGeneration(UUID exercisePerformanceKeyAtGeneration) {
		this.exercisePerformanceKeyAtGeneration = exercisePerformanceKeyAtGeneration;
	}

	void setCurrentFeasible(boolean currentFeasible) {
		this.currentFeasible = currentFeasible;
	}

	void setPrescribedFeasible(boolean prescribedFeasible) {
		this.prescribedFeasible = prescribedFeasible;
	}

	void setPerformedFeasible(boolean performedFeasible) {
		this.performedFeasible = performedFeasible;
	}

	void setAnalysisReasonCode(FeasibilityReasonCode analysisReasonCode) {
		this.analysisReasonCode = analysisReasonCode;
	}

	void setAction(WorkoutAdaptationAction action) {
		this.action = action;
	}

	void setGeneratedTargetExerciseDefinitionId(UUID generatedTargetExerciseDefinitionId) {
		this.generatedTargetExerciseDefinitionId = generatedTargetExerciseDefinitionId;
	}

	void setGeneratedTargetNameSnapshot(String generatedTargetNameSnapshot) {
		this.generatedTargetNameSnapshot = generatedTargetNameSnapshot;
	}

	void setGeneratedRelationshipId(UUID generatedRelationshipId) {
		this.generatedRelationshipId = generatedRelationshipId;
	}

	void setGeneratedRelationshipTypeSnapshot(ExerciseSubstitutionRelationshipType generatedRelationshipTypeSnapshot) {
		this.generatedRelationshipTypeSnapshot = generatedRelationshipTypeSnapshot;
	}

	void setGeneratedCompatibilitySnapshot(ExerciseSubstitutionCompatibility generatedCompatibilitySnapshot) {
		this.generatedCompatibilitySnapshot = generatedCompatibilitySnapshot;
	}

	void setGeneratedRationaleSnapshot(String generatedRationaleSnapshot) {
		this.generatedRationaleSnapshot = generatedRationaleSnapshot;
	}

	void setSelectedTargetExerciseDefinitionId(UUID selectedTargetExerciseDefinitionId) {
		this.selectedTargetExerciseDefinitionId = selectedTargetExerciseDefinitionId;
	}

	void setSelectedRelationshipId(UUID selectedRelationshipId) {
		this.selectedRelationshipId = selectedRelationshipId;
	}

	void setAthleteDecision(WorkoutAdaptationDecision athleteDecision) {
		this.athleteDecision = athleteDecision;
	}

	void setAthleteNotes(String athleteNotes) {
		this.athleteNotes = athleteNotes;
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

	void setMissingRequiredEquipment(Set<EquipmentType> missingRequiredEquipment) {
		this.missingRequiredEquipment = missingRequiredEquipment == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(missingRequiredEquipment);
	}

	void setAlternatives(List<WorkoutAdaptationProposalItemAlternativeJpaEntity> alternatives) {
		this.alternatives = alternatives == null ? new ArrayList<>() : new ArrayList<>(alternatives);
	}

}
