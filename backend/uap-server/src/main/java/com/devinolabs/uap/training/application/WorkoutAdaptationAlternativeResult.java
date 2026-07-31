package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternativeId;

public record WorkoutAdaptationAlternativeResult(
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
}
