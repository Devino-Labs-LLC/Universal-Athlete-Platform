package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;

/**
 * Ranked substitute candidate snapshotted at proposal generation time.
 */
public final class WorkoutAdaptationAlternative {

	private final WorkoutAdaptationAlternativeId id;
	private final int rankPosition;
	private final ExerciseSubstitutionRelationshipId relationshipId;
	private final ExerciseDefinitionId targetExerciseDefinitionId;
	private final String targetNameSnapshot;
	private final ExerciseSubstitutionRelationshipType relationshipTypeSnapshot;
	private final ExerciseSubstitutionCompatibility compatibilitySnapshot;
	private final String rationaleSnapshot;
	private final ExerciseDifficulty targetDifficultySnapshot;
	private final ImpactLevel targetImpactLevelSnapshot;
	private final List<EquipmentType> requiredEquipment;
	private final boolean selectedDefault;

	public WorkoutAdaptationAlternative(
			WorkoutAdaptationAlternativeId id,
			int rankPosition,
			ExerciseSubstitutionRelationshipId relationshipId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			String targetNameSnapshot,
			ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility compatibilitySnapshot,
			String rationaleSnapshot,
			ExerciseDifficulty targetDifficultySnapshot,
			ImpactLevel targetImpactLevelSnapshot,
			List<EquipmentType> requiredEquipment,
			boolean selectedDefault) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		if (rankPosition < 1) {
			throw new IllegalArgumentException("rankPosition must be positive");
		}
		this.rankPosition = rankPosition;
		this.relationshipId = relationshipId;
		this.targetExerciseDefinitionId = Objects.requireNonNull(
				targetExerciseDefinitionId, "targetExerciseDefinitionId must not be null");
		this.targetNameSnapshot = Objects.requireNonNull(targetNameSnapshot, "targetNameSnapshot must not be null");
		this.relationshipTypeSnapshot = relationshipTypeSnapshot;
		this.compatibilitySnapshot = compatibilitySnapshot;
		this.rationaleSnapshot = rationaleSnapshot;
		this.targetDifficultySnapshot = targetDifficultySnapshot;
		this.targetImpactLevelSnapshot = targetImpactLevelSnapshot;
		this.requiredEquipment = requiredEquipment == null ? List.of() : List.copyOf(requiredEquipment);
		this.selectedDefault = selectedDefault;
	}

	public WorkoutAdaptationAlternativeId id() {
		return id;
	}

	public int rankPosition() {
		return rankPosition;
	}

	public ExerciseSubstitutionRelationshipId relationshipId() {
		return relationshipId;
	}

	public ExerciseDefinitionId targetExerciseDefinitionId() {
		return targetExerciseDefinitionId;
	}

	public String targetNameSnapshot() {
		return targetNameSnapshot;
	}

	public ExerciseSubstitutionRelationshipType relationshipTypeSnapshot() {
		return relationshipTypeSnapshot;
	}

	public ExerciseSubstitutionCompatibility compatibilitySnapshot() {
		return compatibilitySnapshot;
	}

	public String rationaleSnapshot() {
		return rationaleSnapshot;
	}

	public ExerciseDifficulty targetDifficultySnapshot() {
		return targetDifficultySnapshot;
	}

	public ImpactLevel targetImpactLevelSnapshot() {
		return targetImpactLevelSnapshot;
	}

	public List<EquipmentType> requiredEquipment() {
		return requiredEquipment;
	}

	public boolean selectedDefault() {
		return selectedDefault;
	}

}
