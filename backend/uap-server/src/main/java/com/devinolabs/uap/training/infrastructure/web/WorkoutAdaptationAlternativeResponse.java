package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.WorkoutAdaptationAlternativeResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ImpactLevel;

record WorkoutAdaptationAlternativeResponse(
		UUID id,
		int rankPosition,
		UUID relationshipId,
		UUID targetExerciseDefinitionId,
		String targetNameSnapshot,
		ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
		ExerciseSubstitutionCompatibility compatibilitySnapshot,
		String rationaleSnapshot,
		ExerciseDifficulty targetDifficultySnapshot,
		ImpactLevel targetImpactLevelSnapshot,
		List<EquipmentType> requiredEquipment,
		boolean selectedDefault) {

	static WorkoutAdaptationAlternativeResponse from(WorkoutAdaptationAlternativeResult result) {
		return new WorkoutAdaptationAlternativeResponse(
				result.id().value(),
				result.rankPosition(),
				result.relationshipId() == null ? null : result.relationshipId().value(),
				result.targetExerciseDefinitionId().value(),
				result.targetNameSnapshot(),
				result.relationshipTypeSnapshot(),
				result.compatibilitySnapshot(),
				result.rationaleSnapshot(),
				result.targetDifficultySnapshot(),
				result.targetImpactLevelSnapshot(),
				result.requiredEquipment(),
				result.selectedDefault());
	}

}
