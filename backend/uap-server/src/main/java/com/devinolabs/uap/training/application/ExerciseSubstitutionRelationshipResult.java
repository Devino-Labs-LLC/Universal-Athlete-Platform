package com.devinolabs.uap.training.application;

import java.time.Instant;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

public record ExerciseSubstitutionRelationshipResult(
		ExerciseSubstitutionRelationshipId id,
		AthleteId ownerAthleteId,
		ExerciseDefinitionId sourceExerciseDefinitionId,
		ExerciseDefinitionId targetExerciseDefinitionId,
		ExerciseSubstitutionRelationshipType relationshipType,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		String rationale,
		boolean active,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {
}
