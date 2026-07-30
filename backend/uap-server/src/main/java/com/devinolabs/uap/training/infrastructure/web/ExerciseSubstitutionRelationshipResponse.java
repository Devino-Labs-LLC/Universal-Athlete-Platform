package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseSubstitutionCandidateResult;
import com.devinolabs.uap.training.application.ExerciseSubstitutionRelationshipResult;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

record ExerciseSubstitutionRelationshipResponse(
		UUID id,
		UUID ownerAthleteId,
		UUID sourceExerciseDefinitionId,
		UUID targetExerciseDefinitionId,
		ExerciseSubstitutionRelationshipType relationshipType,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		String rationale,
		boolean active,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {

	static ExerciseSubstitutionRelationshipResponse from(ExerciseSubstitutionRelationshipResult result) {
		return new ExerciseSubstitutionRelationshipResponse(
				result.id().value(),
				result.ownerAthleteId() == null ? null : result.ownerAthleteId().value(),
				result.sourceExerciseDefinitionId().value(),
				result.targetExerciseDefinitionId().value(),
				result.relationshipType(),
				result.compatibilityLevel(),
				result.rationale(),
				result.active(),
				result.archivedAt(),
				result.createdAt(),
				result.updatedAt());
	}

}

record ExerciseSubstitutionCandidateResponse(
		UUID relationshipId,
		UUID targetExerciseDefinitionId,
		String targetCanonicalName,
		ExerciseSubstitutionRelationshipType relationshipType,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		String rationale,
		UUID trainingEnvironmentId,
		String trainingEnvironmentName) {

	static ExerciseSubstitutionCandidateResponse from(ExerciseSubstitutionCandidateResult result) {
		return new ExerciseSubstitutionCandidateResponse(
				result.relationshipId().value(),
				result.targetExerciseDefinitionId().value(),
				result.targetCanonicalName(),
				result.relationshipType(),
				result.compatibilityLevel(),
				result.rationale(),
				result.trainingEnvironmentId() == null ? null : result.trainingEnvironmentId().value(),
				result.trainingEnvironmentName());
	}

}
