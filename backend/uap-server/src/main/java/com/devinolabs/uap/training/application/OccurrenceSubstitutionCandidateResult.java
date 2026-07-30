package com.devinolabs.uap.training.application;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

public record OccurrenceSubstitutionCandidateResult(
		ExerciseSubstitutionRelationshipId relationshipId,
		ExerciseDefinitionId targetExerciseDefinitionId,
		String targetCanonicalName,
		ExerciseSubstitutionRelationshipType relationshipType,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		String rationale,
		WorkoutOccurrenceEnvironmentContextResult environmentContext) {
}
