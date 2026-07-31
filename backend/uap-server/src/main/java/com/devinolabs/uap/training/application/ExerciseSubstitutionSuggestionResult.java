package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionSuggestionRanker;

public record ExerciseSubstitutionSuggestionResult(
		int rankingPosition,
		ExerciseSubstitutionRelationshipId relationshipId,
		ExerciseDefinitionId targetExerciseDefinitionId,
		String targetCanonicalName,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		ExerciseSubstitutionRelationshipType relationshipType,
		int equipmentBurden,
		int difficultyProximity,
		String rationale,
		List<EquipmentType> targetRequiredEquipment,
		ExerciseDifficulty targetDifficulty) implements ExerciseSubstitutionSuggestionRanker.RankableSuggestion {

	public ExerciseSubstitutionSuggestionResult {
		targetRequiredEquipment = targetRequiredEquipment == null ? List.of() : List.copyOf(targetRequiredEquipment);
	}

}
