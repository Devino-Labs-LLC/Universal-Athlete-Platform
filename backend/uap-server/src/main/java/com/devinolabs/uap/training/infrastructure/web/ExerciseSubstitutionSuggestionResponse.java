package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseSubstitutionSuggestionResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

record ExerciseSubstitutionSuggestionResponse(
		int rankingPosition,
		UUID relationshipId,
		UUID targetExerciseDefinitionId,
		String targetCanonicalName,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		ExerciseSubstitutionRelationshipType relationshipType,
		int equipmentBurden,
		int difficultyProximity,
		String rationale,
		List<EquipmentType> targetRequiredEquipment,
		ExerciseDifficulty targetDifficulty) {

	static ExerciseSubstitutionSuggestionResponse from(ExerciseSubstitutionSuggestionResult result) {
		return new ExerciseSubstitutionSuggestionResponse(
				result.rankingPosition(),
				result.relationshipId().value(),
				result.targetExerciseDefinitionId().value(),
				result.targetCanonicalName(),
				result.compatibilityLevel(),
				result.relationshipType(),
				result.equipmentBurden(),
				result.difficultyProximity(),
				result.rationale(),
				result.targetRequiredEquipment(),
				result.targetDifficulty());
	}

	static List<ExerciseSubstitutionSuggestionResponse> fromList(
			List<ExerciseSubstitutionSuggestionResult> results) {
		return results.stream().map(ExerciseSubstitutionSuggestionResponse::from).toList();
	}

}
