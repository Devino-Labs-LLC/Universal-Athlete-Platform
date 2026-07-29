package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

record CreateExerciseSubstitutionRelationshipRequest(
		@NotNull UUID targetExerciseDefinitionId,
		@NotNull ExerciseSubstitutionRelationshipType relationshipType,
		@NotNull ExerciseSubstitutionCompatibility compatibilityLevel,
		@Size(max = 2000) String rationale) {
}
