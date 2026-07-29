package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;

final class ExerciseSubstitutionRelationshipAccessPolicy {

	private ExerciseSubstitutionRelationshipAccessPolicy() {
	}

	static void assertCreatable(
			AthleteId athleteId,
			ExerciseDefinition source,
			ExerciseDefinition target) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		if (!isAllowedEndpoint(athleteId, source) || !isAllowedEndpoint(athleteId, target)) {
			throw new InvalidExerciseSubstitutionRelationshipOwnershipException(
					"Athletes may only relate their own custom definitions to accessible definitions");
		}
		if (source.scope() == ExerciseDefinitionScope.SYSTEM
				&& target.scope() == ExerciseDefinitionScope.SYSTEM) {
			throw new InvalidExerciseSubstitutionRelationshipOwnershipException(
					"System-to-system substitution relationships are seeded only");
		}
	}

	private static boolean isAllowedEndpoint(AthleteId athleteId, ExerciseDefinition definition) {
		return definition.scope() == ExerciseDefinitionScope.SYSTEM
				|| definition.isOwnedBy(athleteId);
	}

	static boolean isAccessible(AthleteId athleteId, ExerciseSubstitutionRelationship relationship) {
		return relationship.isSystemOwned() || relationship.isOwnedBy(athleteId);
	}

	static ExerciseSubstitutionRelationship requireAccessible(
			ExerciseSubstitutionRelationshipRepository repository,
			AthleteId athleteId,
			com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId relationshipId) {
		ExerciseSubstitutionRelationship relationship = repository
				.findById(relationshipId)
				.orElseThrow(ExerciseSubstitutionRelationshipNotFoundException::new);
		if (!isAccessible(athleteId, relationship)) {
			throw new ExerciseSubstitutionRelationshipNotAccessibleException();
		}
		return relationship;
	}

	static ExerciseSubstitutionRelationship requireOwnedMutable(
			ExerciseSubstitutionRelationshipRepository repository,
			AthleteId athleteId,
			com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId relationshipId) {
		ExerciseSubstitutionRelationship relationship = requireAccessible(repository, athleteId, relationshipId);
		if (relationship.isSystemOwned()) {
			throw new com.devinolabs.uap.training.domain
					.SystemExerciseSubstitutionRelationshipModificationNotAllowedException();
		}
		if (!relationship.isOwnedBy(athleteId)) {
			throw new ExerciseSubstitutionRelationshipNotAccessibleException();
		}
		return relationship;
	}

}
