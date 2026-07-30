package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import com.devinolabs.uap.training.application.OccurrenceSubstitutionCandidateResult;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

public record OccurrenceSubstitutionCandidateResponse(
		UUID relationshipId,
		UUID targetExerciseDefinitionId,
		String targetCanonicalName,
		ExerciseSubstitutionRelationshipType relationshipType,
		ExerciseSubstitutionCompatibility compatibilityLevel,
		String rationale,
		WorkoutOccurrenceEnvironmentSnapshotResponse environmentContext) {

	static OccurrenceSubstitutionCandidateResponse from(OccurrenceSubstitutionCandidateResult result) {
		return new OccurrenceSubstitutionCandidateResponse(
				result.relationshipId().value(),
				result.targetExerciseDefinitionId().value(),
				result.targetCanonicalName(),
				result.relationshipType(),
				result.compatibilityLevel(),
				result.rationale(),
				WorkoutOccurrenceEnvironmentSnapshotResponse.from(result.environmentContext()));
	}
}
