package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ImpactLevel;

@Entity
@Table(name = "workout_adaptation_proposal_item_alternatives")
class WorkoutAdaptationProposalItemAlternativeJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposal_item_id", nullable = false, columnDefinition = "BINARY(16)")
	private WorkoutAdaptationProposalItemJpaEntity proposalItem;

	@Column(name = "rank_position", nullable = false)
	private int rankPosition;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "relationship_id", columnDefinition = "BINARY(16)")
	private UUID relationshipId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "target_exercise_definition_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID targetExerciseDefinitionId;

	@Column(name = "target_name_snapshot", nullable = false, length = 160)
	private String targetNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship_type_snapshot", length = 40)
	private ExerciseSubstitutionRelationshipType relationshipTypeSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "compatibility_snapshot", length = 32)
	private ExerciseSubstitutionCompatibility compatibilitySnapshot;

	@Column(name = "rationale_snapshot", length = 500)
	private String rationaleSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_difficulty_snapshot", length = 32)
	private ExerciseDifficulty targetDifficultySnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_impact_level_snapshot", length = 32)
	private ImpactLevel targetImpactLevelSnapshot;

	@Column(name = "selected_default", nullable = false)
	private boolean selectedDefault;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "workout_adaptation_proposal_item_alternative_equipment",
			joinColumns = @JoinColumn(name = "alternative_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> requiredEquipment = new LinkedHashSet<>();

	protected WorkoutAdaptationProposalItemAlternativeJpaEntity() {
	}

	UUID getId() {
		return id;
	}

	WorkoutAdaptationProposalItemJpaEntity getProposalItem() {
		return proposalItem;
	}

	int getRankPosition() {
		return rankPosition;
	}

	UUID getRelationshipId() {
		return relationshipId;
	}

	UUID getTargetExerciseDefinitionId() {
		return targetExerciseDefinitionId;
	}

	String getTargetNameSnapshot() {
		return targetNameSnapshot;
	}

	ExerciseSubstitutionRelationshipType getRelationshipTypeSnapshot() {
		return relationshipTypeSnapshot;
	}

	ExerciseSubstitutionCompatibility getCompatibilitySnapshot() {
		return compatibilitySnapshot;
	}

	String getRationaleSnapshot() {
		return rationaleSnapshot;
	}

	ExerciseDifficulty getTargetDifficultySnapshot() {
		return targetDifficultySnapshot;
	}

	ImpactLevel getTargetImpactLevelSnapshot() {
		return targetImpactLevelSnapshot;
	}

	boolean isSelectedDefault() {
		return selectedDefault;
	}

	Set<EquipmentType> getRequiredEquipment() {
		return requiredEquipment;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setProposalItem(WorkoutAdaptationProposalItemJpaEntity proposalItem) {
		this.proposalItem = proposalItem;
	}

	void setRankPosition(int rankPosition) {
		this.rankPosition = rankPosition;
	}

	void setRelationshipId(UUID relationshipId) {
		this.relationshipId = relationshipId;
	}

	void setTargetExerciseDefinitionId(UUID targetExerciseDefinitionId) {
		this.targetExerciseDefinitionId = targetExerciseDefinitionId;
	}

	void setTargetNameSnapshot(String targetNameSnapshot) {
		this.targetNameSnapshot = targetNameSnapshot;
	}

	void setRelationshipTypeSnapshot(ExerciseSubstitutionRelationshipType relationshipTypeSnapshot) {
		this.relationshipTypeSnapshot = relationshipTypeSnapshot;
	}

	void setCompatibilitySnapshot(ExerciseSubstitutionCompatibility compatibilitySnapshot) {
		this.compatibilitySnapshot = compatibilitySnapshot;
	}

	void setRationaleSnapshot(String rationaleSnapshot) {
		this.rationaleSnapshot = rationaleSnapshot;
	}

	void setTargetDifficultySnapshot(ExerciseDifficulty targetDifficultySnapshot) {
		this.targetDifficultySnapshot = targetDifficultySnapshot;
	}

	void setTargetImpactLevelSnapshot(ImpactLevel targetImpactLevelSnapshot) {
		this.targetImpactLevelSnapshot = targetImpactLevelSnapshot;
	}

	void setSelectedDefault(boolean selectedDefault) {
		this.selectedDefault = selectedDefault;
	}

	void setRequiredEquipment(Set<EquipmentType> requiredEquipment) {
		this.requiredEquipment = requiredEquipment == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(requiredEquipment);
	}

}
